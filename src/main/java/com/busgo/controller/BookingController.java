package com.busgo.controller;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.Booking;
import com.busgo.exception.ApiException;
import com.busgo.repository.BookingRepository;
import com.busgo.service.BookingService;
import com.busgo.service.CurrentUser;
import com.busgo.service.Mapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;
    private final BookingRepository bookings;
    private final CurrentUser current;
    private final Mapper mapper;

    public BookingController(BookingService service, BookingRepository bookings, CurrentUser current, Mapper mapper) {
        this.service = service; this.bookings = bookings; this.current = current; this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest r, Authentication auth) {
        return mapper.booking(service.create(current.get(auth), r));
    }

    @GetMapping("/my")
    public List<BookingResponse> mine(Authentication auth) {
        return bookings.findByCustomerIdOrderByBookedAtDesc(current.get(auth).getId()).stream().map(mapper::booking).toList();
    }

    @GetMapping("/{id}")
    public BookingResponse one(@PathVariable Long id, Authentication auth) {
        Booking b = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!b.getCustomer().getId().equals(current.get(auth).getId())) throw ApiException.forbidden("This is not your booking");
        return mapper.booking(b);
    }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @RequestBody(required = false) CancelBookingRequest req, Authentication auth) {
        return mapper.booking(service.cancel(current.get(auth), id, req));
    }
}
