package com.busgo.service;

import com.busgo.entity.*;
import com.busgo.repository.BookingRepository;
import com.busgo.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class JourneyReminderService {
    private static final Logger log = LoggerFactory.getLogger(JourneyReminderService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final BookingRepository bookingRepo;
    private final NotificationRepository notificationRepo;

    public JourneyReminderService(BookingRepository bookingRepo, NotificationRepository notificationRepo) {
        this.bookingRepo = bookingRepo;
        this.notificationRepo = notificationRepo;
    }

    /**
     * Periodically checks for confirmed bookings departing within 2 hours
     * and sends automatic journey reminder notifications.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void runScheduledReminderCheck() {
        processPendingReminders();
    }

    /**
     * Checks all pending bookings where departure is within 2 hours from now.
     */
    @Transactional
    public int processPendingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(2);
        LocalDateTime minDeparture = now.minusMinutes(15); // Exclude journeys that departed long ago

        List<Booking> pendingList = bookingRepo.findPendingReminders(threshold, minDeparture);
        int count = 0;
        for (Booking bk : pendingList) {
            if (sendReminder(bk)) {
                count++;
            }
        }
        if (count > 0) {
            log.info("Sent {} automatic journey reminder notifications.", count);
        }
        return count;
    }

    /**
     * Checks if a newly booked journey is departing within 2 hours.
     * If so, sends the journey reminder immediately.
     */
    @Transactional
    public boolean checkAndSendReminderForBooking(Booking bk) {
        if (bk == null || bk.isJourneyReminderSent() || bk.getStatus() != BookingStatus.CONFIRMED) {
            return false;
        }
        Bus bus = bk.getBus();
        if (bus == null || bus.getDepartureTime() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (bus.getDepartureTime().minusHours(2).isBefore(now) && bus.getDepartureTime().isAfter(now.minusMinutes(15))) {
            return sendReminder(bk);
        }
        return false;
    }

    /**
     * Sends the journey reminder notification for a specific booking.
     */
    @Transactional
    public boolean sendReminder(Booking bk) {
        if (bk == null || bk.isJourneyReminderSent()) {
            return false;
        }
        Bus bus = bk.getBus();
        User customer = bk.getCustomer();
        if (bus == null || customer == null) {
            return false;
        }

        List<Integer> seatNums = bk.getSeats().stream()
                .map(BookingSeat::getSeatNumber)
                .sorted()
                .toList();

        String seatDisplay = (seatNums.size() == 1 ? "Seat: " + seatNums.get(0)
                : "Seats: " + seatNums.stream().map(String::valueOf).collect(Collectors.joining(", ")));

        String depTimeStr = bus.getDepartureTime().format(TIME_FORMATTER);

        String details = bus.getSource() + " → " + bus.getDestination() +
                "\nDeparture: " + depTimeStr +
                "\n" + seatDisplay;

        Notification notif = new Notification(
                customer,
                "Your journey starts soon! Have a safe journey.",
                "JOURNEY_REMINDER",
                details
        );
        notificationRepo.save(notif);

        bk.setJourneyReminderSent(true);
        bookingRepo.saveAndFlush(bk);
        log.info("Journey reminder sent for booking {} to customer {}", bk.getBookingCode(), customer.getEmail());
        return true;
    }
}

