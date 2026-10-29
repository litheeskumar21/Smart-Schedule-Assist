package com.smartschedule.controller;

import com.smartschedule.model.Notification;
import com.smartschedule.model.User;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationRestController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationService.subscribe(getCurrentUser());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        long count = notificationService.getUnreadCount(getCurrentUser());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentNotifications() {
        User user = getCurrentUser();
        List<Notification> list = notificationService.getUserNotifications(user);
        List<Map<String, Object>> result = list.stream().limit(10).map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("read", n.isRead());
            m.put("type", n.getType() != null ? n.getType().name() : "SYSTEM");
            m.put("timeAgo", formatTimeAgo(n.getCreatedAt()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private String formatTimeAgo(LocalDateTime dt) {
        if (dt == null) return "";
        Duration d = Duration.between(dt, LocalDateTime.now());
        long mins = d.toMinutes();
        if (mins < 1) return "Just now";
        if (mins < 60) return mins + "m ago";
        long hours = d.toHours();
        if (hours < 24) return hours + "h ago";
        long days = d.toDays();
        if (days == 1) return "Yesterday";
        if (days < 7) return days + "d ago";
        return dt.getMonth().name().substring(0, 3) + " " + dt.getDayOfMonth();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUser());
        return ResponseEntity.ok().build();
    }
}
