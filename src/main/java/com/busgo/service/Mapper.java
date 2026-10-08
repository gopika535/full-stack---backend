package com.busgo.service;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.*;
import com.busgo.repository.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/** Converts entities into API response objects. */
@Component
public class Mapper {
    private final BookingSeatRepository seatRepo;
    private final BookingRepository bookingRepo;
    private final FeedbackRepository feedbackRepo;

    public Mapper(BookingSeatRepository seatRepo, BookingRepository bookingRepo, FeedbackRepository feedbackRepo) {
        this.seatRepo = seatRepo; this.bookingRepo = bookingRepo; this.feedbackRepo = feedbackRepo;
    }

    public static Integer getAdjacentSeatNumber(int seatNumber, int totalSeats) {
        if (seatNumber < 1 || seatNumber > totalSeats) return null;
        int adj;
        if (seatNumber % 4 == 1) adj = seatNumber + 1;
        else if (seatNumber % 4 == 2) adj = seatNumber - 1;
        else if (seatNumber % 4 == 3) adj = seatNumber + 1;
        else adj = seatNumber - 1;

        if (adj >= 1 && adj <= totalSeats) {
            return adj;
        }
        return null;
    }

    public BusResponse bus(Bus b) {
        List<BookingSeat> confirmedSeats = seatRepo.findConfirmedSeatsByBusId(b.getId());
        List<Integer> booked = confirmedSeats.stream().map(BookingSeat::getSeatNumber).distinct().sorted().toList();

        List<Integer> womenBooked = confirmedSeats.stream()
                .filter(bs -> bs.isWomenPreference() || "Female".equalsIgnoreCase(bs.getPassengerGender()))
                .map(BookingSeat::getSeatNumber)
                .distinct().sorted().toList();

        List<Integer> womenPreferred = new java.util.ArrayList<>();
        for (BookingSeat bs : confirmedSeats) {
            if (bs.isWomenPreference() || "Female".equalsIgnoreCase(bs.getPassengerGender())) {
                Integer adj = getAdjacentSeatNumber(bs.getSeatNumber(), b.getTotalSeats());
                if (adj != null && !booked.contains(adj) && !womenPreferred.contains(adj)) {
                    womenPreferred.add(adj);
                }
            }
        }
        java.util.Collections.sort(womenPreferred);

        boolean active = b.getStatus() == BusStatus.ACTIVE;
        int available = active ? Math.max(0, b.getTotalSeats() - booked.size()) : 0;
        return new BusResponse(b.getId(), b.getName(), b.getSource(), b.getDestination(),
                b.getDepartureTime(), b.getDepartureTime().plusMinutes(b.getDurationMinutes()), b.getDurationMinutes(),
                b.getTotalSeats(), available, booked.size(), b.getFare(), b.getBusType(), b.getAmenities(),
                b.getStatus().name(), b.getOperator().getId(), b.getOperator().getName(),
                bookingRepo.countByBusId(b.getId()), booked, womenPreferred, womenBooked);
    }

    public BookingResponse booking(Booking bk) {
        Bus bus = bk.getBus();
        User c = bk.getCustomer();
        LocalDateTime arrival = bus.getDepartureTime().plusMinutes(bus.getDurationMinutes());
        boolean completed = bk.getStatus() == BookingStatus.CONFIRMED && arrival.isBefore(LocalDateTime.now());
        Feedback f = feedbackRepo.findByBookingId(bk.getId()).orElse(null);
        List<Integer> seats = bk.getSeats().stream().map(BookingSeat::getSeatNumber).sorted().toList();
        List<PassengerResponse> passengers = bk.getSeats().stream()
                .map(s -> {
                    String g = s.getPassengerGender();
                    if (g == null || g.isBlank() || "Prefer not to say".equalsIgnoreCase(g)) {
                        g = s.isWomenPreference() ? "Female" : "Female";
                    }
                    return new PassengerResponse(
                            s.getSeatNumber(),
                            s.getPassengerName() != null ? s.getPassengerName() : c.getName(),
                            g,
                            s.getPassengerType() != null ? s.getPassengerType() : "General Passenger",
                            s.isWomenPreference()
                    );
                })
                .toList();
        return new BookingResponse(bk.getId(), bk.getBookingCode(), bus.getId(), bus.getName(), bus.getSource(),
                bus.getDestination(), bus.getDepartureTime(), arrival, bus.getOperator().getName(),
                c.getId(), c.getName(), c.getEmail(), c.getMobile(), seats, passengers, bk.getTotalFare(),
                bk.getStatus().name(), bk.getBookedAt(), completed, f != null,
                f == null ? null : f.getRating(), f == null ? null : f.getComment(),
                bk.isMedicalAssistance(), bk.getMedicalIssueDetails(), bk.isJourneyReminderSent());
    }

    public NotificationResponse notification(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getType(), n.getDetails(), n.isReadFlag(), n.getCreatedAt());
    }

    public FeedbackResponse feedback(Feedback f) {
        Bus b = f.getBus();
        return new FeedbackResponse(f.getId(), f.getBooking().getId(), f.getBooking().getBookingCode(),
                f.getCustomer().getName(), f.getCustomer().getEmail(), b.getName(),
                b.getSource() + " → " + b.getDestination(), f.getRating(), f.getComment(), f.getCreatedAt());
    }
}
