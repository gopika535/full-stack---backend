package com.busgo.controller;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.*;
import com.busgo.exception.ApiException;
import com.busgo.repository.*;
import com.busgo.service.Mapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final BusRepository buses;
    private final BookingRepository bookings;
    private final FeedbackRepository feedbacks;
    private final PasswordEncoder encoder;
    private final Mapper mapper;

    public AdminController(UserRepository users, BusRepository buses, BookingRepository bookings,
                           FeedbackRepository feedbacks, PasswordEncoder encoder, Mapper mapper) {
        this.users = users; this.buses = buses; this.bookings = bookings;
        this.feedbacks = feedbacks; this.encoder = encoder; this.mapper = mapper;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(users.countByRole(Role.CUSTOMER), users.countByRole(Role.OPERATOR), buses.count(),
                buses.countByStatus(BusStatus.ACTIVE), buses.countByStatus(BusStatus.CANCELLED),
                bookings.count(), bookings.countConfirmed(), bookings.totalRevenue(), feedbacks.count());
    }

    // ----- customers -----
    @GetMapping("/users")
    public List<UserResponse> customers() {
        return users.findByRoleOrderByIdDesc(Role.CUSTOMER).stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getMobile(), u.getRole().name(),
                        bookings.countByCustomerId(u.getId()))).toList();
    }

    @GetMapping("/users/{id}/bookings")
    public List<BookingResponse> customerBookings(@PathVariable Long id) {
        return bookings.findByCustomerIdOrderByBookedAtDesc(id).stream().map(mapper::booking).toList();
    }

    // ----- operators (unlimited) -----
    private UserResponse op(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getMobile(), u.getRole().name(), buses.countByOperatorId(u.getId()));
    }

    @GetMapping("/operators")
    public List<UserResponse> operators() {
        return users.findByRoleOrderByIdDesc(Role.OPERATOR).stream().map(this::op).toList();
    }

    @PostMapping("/operators")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse addOperator(@Valid @RequestBody OperatorRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("This email is already registered");
        if (r.password() == null || r.password().length() < 6) throw ApiException.badRequest("Password must be at least 6 characters");
        return op(users.save(new User(r.name().trim(), email, r.mobile().trim(), encoder.encode(r.password()), Role.OPERATOR)));
    }

    @PutMapping("/operators/{id}")
    public UserResponse updateOperator(@PathVariable Long id, @Valid @RequestBody OperatorRequest r) {
        User u = users.findById(id).filter(x -> x.getRole() == Role.OPERATOR)
                .orElseThrow(() -> ApiException.notFound("Operator not found"));
        String email = r.email().trim().toLowerCase();
        if (!email.equals(u.getEmail()) && users.existsByEmail(email)) throw ApiException.conflict("This email is already registered");
        u.setName(r.name().trim()); u.setEmail(email); u.setMobile(r.mobile().trim());
        if (r.password() != null && !r.password().isBlank()) {
            if (r.password().length() < 6) throw ApiException.badRequest("Password must be at least 6 characters");
            u.setPassword(encoder.encode(r.password()));
        }
        return op(users.save(u));
    }

    @DeleteMapping("/operators/{id}")
    public Map<String, String> deleteOperator(@PathVariable Long id) {
        User u = users.findById(id).filter(x -> x.getRole() == Role.OPERATOR)
                .orElseThrow(() -> ApiException.notFound("Operator not found"));
        if (buses.countByOperatorId(id) > 0)
            throw ApiException.badRequest("This operator still has buses. Reassign them to another operator first.");
        users.delete(u);
        return Map.of("message", "Operator removed");
    }

    // ----- bookings & feedback -----
    @GetMapping("/bookings")
    public List<BookingResponse> allBookings() {
        return bookings.findAllByOrderByBookedAtDesc().stream().map(mapper::booking).toList();
    }

    @GetMapping("/feedback")
    public List<FeedbackResponse> feedback() {
        return feedbacks.findAllByOrderByCreatedAtDesc().stream().map(mapper::feedback).toList();
    }
}
