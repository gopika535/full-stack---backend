package com.busgo.service;

import com.busgo.dto.Dtos.BusRequest;
import com.busgo.entity.*;
import com.busgo.exception.ApiException;
import com.busgo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BusService {
    public static final String CANCEL_MESSAGE = "Sorry, your bus has been cancelled.";

    private final BusRepository busRepo;
    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final BookingSeatRepository seatRepo;
    private final NotificationRepository notificationRepo;

    public BusService(BusRepository busRepo, UserRepository userRepo, BookingRepository bookingRepo,
                      BookingSeatRepository seatRepo, NotificationRepository notificationRepo) {
        this.busRepo = busRepo; this.userRepo = userRepo; this.bookingRepo = bookingRepo;
        this.seatRepo = seatRepo; this.notificationRepo = notificationRepo;
    }

    private User operator(Long id) {
        User op = userRepo.findById(id).orElseThrow(() -> ApiException.badRequest("Operator not found"));
        if (op.getRole() != Role.OPERATOR) throw ApiException.badRequest("Selected user is not an operator");
        return op;
    }

    private void apply(Bus bus, BusRequest r) {
        if (r.source().trim().equalsIgnoreCase(r.destination().trim()))
            throw ApiException.badRequest("Source and destination must be different");
        bus.setName(r.name().trim());
        bus.setSource(r.source().trim());
        bus.setDestination(r.destination().trim());
        bus.setDepartureTime(r.departureTime());
        bus.setDurationMinutes(r.durationMinutes());
        bus.setTotalSeats(r.totalSeats());
        bus.setFare(r.fare());
        bus.setBusType(r.busType());
        bus.setAmenities(r.amenities());
        bus.setOperator(operator(r.operatorId()));
    }

    @Transactional
    public Bus create(BusRequest r) {
        Bus bus = new Bus();
        apply(bus, r);
        bus.setStatus(BusStatus.ACTIVE);
        return busRepo.save(bus);
    }

    /** ONE edit operation: bus details + assigned operator. */
    @Transactional
    public Bus update(Long id, BusRequest r) {
        Bus bus = busRepo.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Bus not found"));
        if (bus.getStatus() == BusStatus.CANCELLED) throw ApiException.badRequest("A cancelled bus cannot be edited");
        List<Integer> booked = seatRepo.findBookedSeatNumbers(id);
        int maxBooked = booked.stream().mapToInt(Integer::intValue).max().orElse(0);
        if (r.totalSeats() < maxBooked)
            throw ApiException.badRequest("Seat " + maxBooked + " is already booked, so total seats cannot be below " + maxBooked);
        boolean timeChanged = !bus.getDepartureTime().equals(r.departureTime());
        apply(bus, r);
        Bus saved = busRepo.save(bus);
        if (timeChanged) {
            for (Booking bk : bookingRepo.findByBusIdOrderByBookedAtDesc(id)) {
                if (bk.getStatus() == BookingStatus.CONFIRMED) {
                    notificationRepo.save(new Notification(bk.getCustomer(),
                            "Your bus " + saved.getName() + " has been rescheduled.", "BUS_UPDATED",
                            "Booking " + bk.getBookingCode() + " · new departure " + saved.getDepartureTime()));
                }
            }
        }
        return saved;
    }

    /** Permanently deletes a bus without bookings; otherwise marks it CANCELLED and notifies every affected customer. */
    @Transactional
    public String cancelOrDelete(Long id) {
        Bus bus = busRepo.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Bus not found"));
        List<Booking> bookings = bookingRepo.findByBusIdOrderByBookedAtDesc(id);
        if (bookings.isEmpty()) {
            busRepo.delete(bus);
            return "Bus deleted permanently (it had no bookings).";
        }
        if (bus.getStatus() == BusStatus.CANCELLED) return "Bus is already cancelled.";
        bus.setStatus(BusStatus.CANCELLED);
        busRepo.save(bus);
        int notified = 0;
        for (Booking bk : bookings) {
            if (bk.getStatus() == BookingStatus.CONFIRMED) {
                bk.setStatus(BookingStatus.CANCELLED);
                bookingRepo.save(bk);
                notificationRepo.save(new Notification(bk.getCustomer(), CANCEL_MESSAGE, "BUS_CANCELLED",
                        bus.getName() + " (" + bus.getSource() + " → " + bus.getDestination() + ") · Booking " + bk.getBookingCode()));
                notified++;
            }
        }
        return "Bus cancelled. Booking history kept; " + notified + " booking(s) cancelled and customers notified.";
    }
}
