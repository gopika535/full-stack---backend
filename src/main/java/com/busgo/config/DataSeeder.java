package com.busgo.config;

import com.busgo.entity.*;
import com.busgo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Seeds one ADMIN, four operators and sample buses the first time the app starts on an empty database. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final BusRepository buses;
    private final BookingRepository bookings;
    private final NotificationRepository notifications;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, BusRepository buses, BookingRepository bookings,
                      NotificationRepository notifications, PasswordEncoder encoder) {
        this.users = users; this.buses = buses; this.bookings = bookings;
        this.notifications = notifications; this.encoder = encoder;
    }

    private User user(String name, String email, String mobile, String pwd, Role role) {
        return users.findByEmail(email).orElseGet(() -> users.save(new User(name, email, mobile, encoder.encode(pwd), role)));
    }

    private Bus bus(String name, User op, String from, String to, LocalDateTime dep, int mins, int seats,
                    double fare, String type, String amenities) {
        Bus b = new Bus();
        b.setName(name); b.setOperator(op); b.setSource(from); b.setDestination(to);
        b.setDepartureTime(dep); b.setDurationMinutes(mins); b.setTotalSeats(seats); b.setFare(fare);
        b.setBusType(type); b.setAmenities(amenities); b.setStatus(BusStatus.ACTIVE);
        return buses.save(b);
    }

    @Override
    public void run(String... args) {
        // exactly one admin
        user("Admin", "admin@busgo.com", "9000000000", "Admin@123", Role.ADMIN);

        User ram = user("Ram", "ram@busgo.com", "9000000001", "Operator@123", Role.OPERATOR);
        User kumar = user("Kumar", "kumar@busgo.com", "9000000002", "Operator@123", Role.OPERATOR);
        User suresh = user("Suresh", "suresh@busgo.com", "9000000003", "Operator@123", Role.OPERATOR);
        User arun = user("Arun", "arun@busgo.com", "9000000004", "Operator@123", Role.OPERATOR);
        User demo = user("Demo Customer", "customer@busgo.com", "9876543210", "Customer@123", Role.CUSTOMER);

        if (buses.count() > 0) return;
        LocalDate today = LocalDate.now();
        bus("KP Travels", ram, "Chennai", "Bangalore", today.plusDays(1).atTime(21, 30), 390, 40, 750,
                "AC Sleeper", "WiFi,Charging Point,Blanket,Water Bottle");
        bus("A1 Travels", ram, "Chennai", "Coimbatore", today.plusDays(1).atTime(22, 0), 460, 36, 850,
                "AC Semi-Sleeper", "Charging Point,Reading Light,Water Bottle");
        bus("Express Travels", kumar, "Coimbatore", "Chennai", today.plusDays(2).atTime(20, 45), 480, 40, 800,
                "Volvo Multi-Axle AC", "WiFi,Charging Point,Snacks,Blanket");
        bus("Super Fast", suresh, "Madurai", "Chennai", today.plusDays(2).atTime(21, 15), 500, 44, 650,
                "Non-AC Seater", "Charging Point,Water Bottle");
        bus("Ayappa Travels", arun, "Chennai", "Madurai", today.plusDays(3).atTime(22, 30), 480, 40, 900,
                "AC Sleeper", "WiFi,Charging Point,Blanket,Entertainment Screen");

        // A journey that is already completed so the rating/feedback flow can be tried immediately.
        Bus done = bus("Sunrise Express", kumar, "Bangalore", "Chennai", today.minusDays(2).atTime(6, 0), 360, 30, 600,
                "AC Seater", "Charging Point,Water Bottle");
        Booking bk = new Booking();
        bk.setBookingCode("BGDEMO001");
        bk.setCustomer(demo); bk.setBus(done); bk.setTotalFare(done.getFare() * 2);
        bk.setStatus(BookingStatus.CONFIRMED); bk.setBookedAt(LocalDateTime.now().minusDays(5));
        bk.getSeats().add(new BookingSeat(bk, done, 3));
        bk.getSeats().add(new BookingSeat(bk, done, 4));
        bookings.save(bk);
        notifications.save(new Notification(demo, "Ticket booked successfully. Booking ID: BGDEMO001", "BOOKING_CONFIRMED",
                "Sunrise Express · Bangalore → Chennai · Seats [3, 4]"));
    }
}
