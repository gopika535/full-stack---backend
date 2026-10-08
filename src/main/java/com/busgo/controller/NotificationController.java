package com.busgo.controller;

import com.busgo.dto.Dtos.NotificationResponse;
import com.busgo.entity.Notification;
import com.busgo.entity.User;
import com.busgo.exception.ApiException;
import com.busgo.repository.NotificationRepository;
import com.busgo.service.CurrentUser;
import com.busgo.service.Mapper;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repo;
    private final CurrentUser current;
    private final Mapper mapper;
    private final com.busgo.service.JourneyReminderService reminderService;

    public NotificationController(NotificationRepository repo, CurrentUser current, Mapper mapper,
                                  com.busgo.service.JourneyReminderService reminderService) {
        this.repo = repo; this.current = current; this.mapper = mapper;
        this.reminderService = reminderService;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication auth) {
        return repo.findByUserIdOrderByCreatedAtDesc(current.get(auth).getId()).stream().map(mapper::notification).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(Authentication auth) {
        return Map.of("count", repo.countByUserIdAndReadFlagFalse(current.get(auth).getId()));
    }

    @PutMapping("/read-all")
    public Map<String, String> readAll(Authentication auth) {
        User u = current.get(auth);
        List<Notification> list = repo.findByUserIdOrderByCreatedAtDesc(u.getId());
        list.forEach(n -> n.setReadFlag(true));
        repo.saveAll(list);
        return Map.of("message", "All notifications marked as read");
    }

    @PutMapping("/{id}/read")
    public NotificationResponse read(@PathVariable Long id, Authentication auth) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(current.get(auth).getId())) throw ApiException.forbidden("Not your notification");
        n.setReadFlag(true);
        return mapper.notification(repo.save(n));
    }

    @PostMapping("/process-reminders")
    public Map<String, Object> processReminders() {
        int count = reminderService.processPendingReminders();
        return Map.of("sentCount", count, "message", "Processed journey reminders successfully");
    }
}
