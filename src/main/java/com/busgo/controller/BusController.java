package com.busgo.controller;

import com.busgo.dto.Dtos.*;
import com.busgo.entity.Bus;
import com.busgo.exception.ApiException;
import com.busgo.repository.BusRepository;
import com.busgo.service.BusService;
import com.busgo.service.CurrentUser;
import com.busgo.service.Mapper;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buses")
public class BusController {
    private final BusRepository busRepo;
    private final BusService busService;
    private final Mapper mapper;
    private final CurrentUser current;

    public BusController(BusRepository busRepo, BusService busService, Mapper mapper, CurrentUser current) {
        this.busRepo = busRepo; this.busService = busService; this.mapper = mapper; this.current = current;
    }

    /** ADMIN: every bus (incl. cancelled). CUSTOMER: all upcoming, active buses. */
    @GetMapping
    public List<BusResponse> all(Authentication auth) {
        if (current.hasRole(auth, "ADMIN"))
            return busRepo.findAllByOrderByDepartureTimeDesc().stream().map(mapper::bus).toList();
        return doSearch("", "", null);
    }

    @GetMapping("/search")
    public List<BusResponse> search(@RequestParam(required = false, defaultValue = "") String from,
                                    @RequestParam(required = false, defaultValue = "") String to,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doSearch(from.trim(), to.trim(), date);
    }

    private List<BusResponse> doSearch(String from, String to, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = date == null ? now : date.atStartOfDay();
        LocalDateTime end = date == null ? now.plusYears(10) : date.plusDays(1).atStartOfDay();
        return busRepo.search(from, to, start, end, now).stream().map(mapper::bus).toList();
    }

    @GetMapping("/{id}")
    public BusResponse one(@PathVariable Long id, Authentication auth) {
        Bus bus = busRepo.findById(id).orElseThrow(() -> ApiException.notFound("Bus not found"));
        if (current.hasRole(auth, "OPERATOR") && !bus.getOperator().getId().equals(current.get(auth).getId()))
            throw ApiException.forbidden("This bus is not assigned to you");
        return mapper.bus(bus);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusResponse create(@Valid @RequestBody BusRequest r) { return mapper.bus(busService.create(r)); }

    @PutMapping("/{id}")
    public BusResponse update(@PathVariable Long id, @Valid @RequestBody BusRequest r) {
        return mapper.bus(busService.update(id, r));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        return Map.of("message", busService.cancelOrDelete(id));
    }
}
