package com.empay.auth.controller;

import com.empay.auth.model.Notification;
import com.empay.auth.model.User;
import com.empay.auth.repository.NotificationRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User user = userOpt.get();
        long unread = notificationRepository.countByUserAndReadFalse(user);
        List<Map<String, Object>> items = notificationRepository.findByUserOrderByCreatedAtDesc(user)
            .stream().map(n -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", n.getId().toString());
                m.put("message", n.getMessage());
                m.put("type", n.getType());
                m.put("read", n.isRead());
                m.put("createdAt", n.getCreatedAt().toString());
                return m;
            }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("unread", unread);
        result.put("items", items);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable UUID id) {
        Optional<Notification> nOpt = notificationRepository.findById(id);
        if (nOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Not found."));

        Notification n = nOpt.get();
        n.setRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok(Map.of("message", "Marked as read."));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findByEmail(body.get("email"));
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        notificationRepository.findByUserOrderByCreatedAtDesc(userOpt.get())
            .stream().filter(n -> !n.isRead())
            .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
        return ResponseEntity.ok(Map.of("message", "All marked as read."));
    }
}
