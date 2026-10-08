package com.busgo.controller;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.Bus;
import com.busgo.entity.User;
import com.busgo.exception.ApiException;
import com.busgo.repository.BookingRepository;
import com.busgo.repository.BusRepository;
import com.busgo.service.CurrentUser;
import com.busgo.service.Mapper;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Everything here is scoped to the logged-in operator's own buses. */
@RestController
@RequestMapping("/api/operator")
public class OperatorController {
    private final BusRepository buses;
    private final BookingRepository bookings;
    private final CurrentUser current;
    private final Mapper mapper;

    public OperatorController(BusRepository buses, BookingRepository bookings, CurrentUser current, Mapper mapper) {
        this.buses = buses; this.bookings = bookings; this.current = current; this.mapper = mapper;
    }

    @GetMapping("/buses")
    public List<BusResponse> myBuses(Authentication auth) {
        User op = current.get(auth);
        return buses.findByOperatorIdOrderByDepartureTimeAsc(op.getId()).stream().map(mapper::bus).toList();
    }

    @GetMapping("/buses/{id}")
    public OperatorBusDetail busDetail(@PathVariable Long id, Authentication auth) {
        User op = current.get(auth);
        Bus bus = buses.findById(id).orElseThrow(() -> ApiException.notFound("Bus not found"));
        if (!bus.getOperator().getId().equals(op.getId())) throw ApiException.forbidden("This bus is not assigned to you");
        return new OperatorBusDetail(mapper.bus(bus),
                bookings.findByBusIdOrderByBookedAtDesc(id).stream().map(mapper::booking).toList());
    }

    @GetMapping("/bookings")
    public List<BookingResponse> myBookings(Authentication auth) {
        User op = current.get(auth);
        return bookings.findByBusOperatorIdOrderByBookedAtDesc(op.getId()).stream().map(mapper::booking).toList();
    }
}
