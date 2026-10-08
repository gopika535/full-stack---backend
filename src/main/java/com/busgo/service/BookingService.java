package com.busgo.service;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.*;
import com.busgo.exception.ApiException;
import com.busgo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingService {
    private final BusRepository busRepo;
    private final BookingRepository bookingRepo;
    private final BookingSeatRepository seatRepo;
    private final NotificationRepository notificationRepo;
    private final FeedbackRepository feedbackRepo;
    private final JourneyReminderService journeyReminderService;

    public BookingService(BusRepository busRepo, BookingRepository bookingRepo, BookingSeatRepository seatRepo,
                          NotificationRepository notificationRepo, FeedbackRepository feedbackRepo) {
        this(busRepo, bookingRepo, seatRepo, notificationRepo, feedbackRepo, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public BookingService(BusRepository busRepo, BookingRepository bookingRepo, BookingSeatRepository seatRepo,
                          NotificationRepository notificationRepo, FeedbackRepository feedbackRepo,
                          JourneyReminderService journeyReminderService) {
        this.busRepo = busRepo; this.bookingRepo = bookingRepo; this.seatRepo = seatRepo;
        this.notificationRepo = notificationRepo; this.feedbackRepo = feedbackRepo;
        this.journeyReminderService = journeyReminderService;
    }

    /**
     * Double-booking protection (3 layers):
     *  1. the bus row is locked (SELECT ... FOR UPDATE) for the whole transaction,
     *  2. requested seats are checked against confirmed seats,
     *  3. a unique (bus_id, seat_number) DB constraint is the final guard.
     */
    @Transactional
    public Booking create(User customer, BookingRequest req) {
        List<PassengerRequest> passengerList = req.passengers();
        if (passengerList == null || passengerList.isEmpty()) {
            if (req.seatNumbers() == null || req.seatNumbers().isEmpty()) {
                throw ApiException.badRequest("Select at least one seat");
            }
            passengerList = new ArrayList<>();
            for (Integer s : req.seatNumbers()) {
                passengerList.add(new PassengerRequest(s, customer.getName(), "Prefer not to say", "General Passenger", false));
            }
        }

        Set<Integer> seats = new TreeSet<>();
        for (PassengerRequest p : passengerList) {
            if (p.seatNumber() == null) throw ApiException.badRequest("Seat number is required for each passenger");
            seats.add(p.seatNumber());
        }

        Bus bus = busRepo.findByIdForUpdate(req.busId()).orElseThrow(() -> ApiException.notFound("Bus not found"));
        if (bus.getStatus() != BusStatus.ACTIVE) throw ApiException.badRequest("This bus has been cancelled");
        if (!bus.getDepartureTime().isAfter(LocalDateTime.now())) throw ApiException.badRequest("This bus has already departed");
        for (int s : seats)
            if (s < 1 || s > bus.getTotalSeats()) throw ApiException.badRequest("Seat " + s + " does not exist on this bus");

        List<BookingSeat> confirmedSeats = seatRepo.findConfirmedSeatsByBusId(bus.getId());
        Set<Integer> bookedSeats = new TreeSet<>(confirmedSeats.stream().map(BookingSeat::getSeatNumber).toList());

        Set<Integer> taken = new TreeSet<>(bookedSeats);
        taken.retainAll(seats);
        if (!taken.isEmpty())
            throw ApiException.conflict("Seat(s) " + taken + " already booked. Please choose different seats.");

        Set<Integer> womenPreferredSet = new HashSet<>();
        for (BookingSeat bs : confirmedSeats) {
            if (bs.isWomenPreference() || "Female".equalsIgnoreCase(bs.getPassengerGender())) {
                Integer adj = Mapper.getAdjacentSeatNumber(bs.getSeatNumber(), bus.getTotalSeats());
                if (adj != null && !bookedSeats.contains(adj)) {
                    womenPreferredSet.add(adj);
                }
            }
        }

        for (PassengerRequest p : passengerList) {
            if (womenPreferredSet.contains(p.seatNumber())) {
                boolean isFemale = "Female".equalsIgnoreCase(p.gender()) || p.womenPreference();
                if ("Male".equalsIgnoreCase(p.gender()) || !isFemale) {
                    throw ApiException.badRequest("warning: prefered for womens only");
                }
            }
        }

        // Also prevent a male passenger from booking adjacent to a female in the same booking if women preference is selected
        for (PassengerRequest p1 : passengerList) {
            if (p1.womenPreference()) {
                Integer adj = Mapper.getAdjacentSeatNumber(p1.seatNumber(), bus.getTotalSeats());
                if (adj != null) {
                    for (PassengerRequest p2 : passengerList) {
                        if (p2.seatNumber() == adj && "Male".equalsIgnoreCase(p2.gender())) {
                            throw ApiException.badRequest("warning: prefered for womens only");
                        }
                    }
                }
            }
        }

        Booking bk = new Booking();
        bk.setBookingCode("BG" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        bk.setCustomer(customer);
        bk.setBus(bus);
        bk.setTotalFare(bus.getFare() * seats.size());
        bk.setStatus(BookingStatus.CONFIRMED);

        for (PassengerRequest p : passengerList) {
            String name = (p.name() != null && !p.name().isBlank()) ? p.name().trim() : customer.getName();
            String gender = (p.gender() != null && !p.gender().isBlank()) ? p.gender().trim() : "Female";
            if (p.womenPreference() || womenPreferredSet.contains(p.seatNumber())) {
                gender = "Female";
            } else if ("Prefer not to say".equalsIgnoreCase(gender)) {
                gender = "Female";
            }
            String type = (p.passengerType() != null && !p.passengerType().isBlank()) ? p.passengerType().trim() : "General Passenger";
            boolean isWomenPref = p.womenPreference() || "Female".equalsIgnoreCase(gender);
            bk.getSeats().add(new BookingSeat(bk, bus, p.seatNumber(), name, gender, type, isWomenPref));
        }

        bk.setMedicalAssistance(req.medicalAssistance());
        bk.setMedicalIssueDetails(req.medicalAssistance() && req.medicalIssueDetails() != null ? req.medicalIssueDetails().trim() : null);

        Booking saved = bookingRepo.saveAndFlush(bk);

        String notifDetails = bus.getName() + " · " + bus.getSource() + " → " + bus.getDestination() + " · Seats " + seats;
        if (bk.isMedicalAssistance() && bk.getMedicalIssueDetails() != null) {
            notifDetails += " · 🚑 Medical Assistance: " + bk.getMedicalIssueDetails();
        }

        notificationRepo.save(new Notification(customer,
                "Ticket booked successfully. Booking ID: " + saved.getBookingCode(), "BOOKING_CONFIRMED",
                notifDetails));

        if (journeyReminderService != null) {
            journeyReminderService.checkAndSendReminderForBooking(saved);
        }

        return saved;
    }

    @Transactional
    public Feedback addFeedback(User customer, FeedbackRequest req) {
        Booking bk = bookingRepo.findById(req.bookingId()).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!bk.getCustomer().getId().equals(customer.getId())) throw ApiException.forbidden("This is not your booking");
        if (bk.getStatus() != BookingStatus.CONFIRMED) throw ApiException.badRequest("Only confirmed bookings can be reviewed");
        Bus bus = bk.getBus();
        if (!bus.getDepartureTime().plusMinutes(bus.getDurationMinutes()).isBefore(LocalDateTime.now()))
            throw ApiException.badRequest("You can give feedback after the journey is completed");
        if (feedbackRepo.existsByBookingId(bk.getId())) throw ApiException.conflict("You already gave feedback for this booking");
        Feedback f = new Feedback();
        f.setBooking(bk); f.setCustomer(customer); f.setBus(bus);
        f.setRating(req.rating());
        f.setComment(req.comment() == null ? "" : req.comment().trim());
        return feedbackRepo.save(f);
    }

    @Transactional
    public Booking cancel(User customer, Long bookingId) {
        return cancel(customer, bookingId, null);
    }

    @Transactional
    public Booking cancel(User customer, Long bookingId, CancelBookingRequest req) {
        Booking bk = bookingRepo.findById(bookingId).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!bk.getCustomer().getId().equals(customer.getId()) && customer.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You are not allowed to cancel this booking");
        }
        if (bk.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.badRequest("This booking is already cancelled");
        }
        Bus bus = bk.getBus();
        if (!bus.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Cannot cancel ticket after bus has departed");
        }

        List<BookingSeat> seatsToCancel;
        if (req != null && req.seatNumbers() != null && !req.seatNumbers().isEmpty()) {
            seatsToCancel = bk.getSeats().stream()
                    .filter(s -> req.seatNumbers().contains(s.getSeatNumber()))
                    .toList();
            if (seatsToCancel.isEmpty()) {
                throw ApiException.badRequest("Selected seats are not found in this booking");
            }
        } else if (req != null && req.ticketCount() != null && req.ticketCount() > 0 && req.ticketCount() < bk.getSeats().size()) {
            seatsToCancel = bk.getSeats().stream().limit(req.ticketCount()).toList();
        } else {
            seatsToCancel = new ArrayList<>(bk.getSeats());
        }

        double refundAmount = bus.getFare() * seatsToCancel.size();
        List<Integer> cancelledSeatNumbers = seatsToCancel.stream().map(BookingSeat::getSeatNumber).sorted().toList();

        seatRepo.deleteAll(seatsToCancel);
        bk.getSeats().removeAll(seatsToCancel);

        if (bk.getSeats().isEmpty()) {
            bk.setStatus(BookingStatus.CANCELLED);
            bk.setTotalFare(0.0);
        } else {
            bk.setTotalFare(Math.max(0.0, bk.getTotalFare() - refundAmount));
        }

        Booking saved = bookingRepo.saveAndFlush(bk);

        String msg = bk.getSeats().isEmpty()
                ? "Ticket cancelled successfully. Booking ID: " + saved.getBookingCode() + ". Refund: ₹" + (int)refundAmount
                : "Partial cancellation: " + cancelledSeatNumbers.size() + " ticket(s) cancelled (Seats " + cancelledSeatNumbers + "). Refund: ₹" + (int)refundAmount;

        notificationRepo.save(new Notification(
                bk.getCustomer(),
                msg,
                "BOOKING_CANCELLED",
                bus.getName() + " · " + bus.getSource() + " → " + bus.getDestination() + " · Seats released: " + cancelledSeatNumbers
        ));
        return saved;
    }
}
