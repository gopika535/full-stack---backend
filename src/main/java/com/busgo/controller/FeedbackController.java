package com.busgo.controller;

import com.busgo.dto.Dtos.*;
import com.busgo.service.BookingService;
import com.busgo.service.CurrentUser;
import com.busgo.service.Mapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Admin reads feedback through GET /api/admin/feedback. */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final BookingService service;
    private final CurrentUser current;
    private final Mapper mapper;

    public FeedbackController(BookingService service, CurrentUser current, Mapper mapper) {
        this.service = service; this.current = current; this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@Valid @RequestBody FeedbackRequest r, Authentication auth) {
        return mapper.feedback(service.addFeedback(current.get(auth), r));
    }
}
