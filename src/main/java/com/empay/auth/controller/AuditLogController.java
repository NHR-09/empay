package com.empay.auth.controller;

import com.empay.auth.model.AuditLog;
import com.empay.auth.model.User;
import com.empay.auth.repository.AuditLogRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogController(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam String email, @RequestParam(required = false) String module) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        String role = userOpt.get().getRole().getRoleName();
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required."));
        }

        List<AuditLog> logs;
        if (module != null && !module.isBlank()) {
            logs = auditLogRepository.findByModuleOrderByCreatedAtDesc(module);
        } else {
            logs = auditLogRepository.findTop100ByOrderByCreatedAtDesc();
        }

        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId().toString());
            m.put("user", l.getUser() != null ? l.getUser().getFirstName() + " " + l.getUser().getLastName() : "System");
            m.put("action", l.getAction());
            m.put("module", l.getModule());
            m.put("oldValue", l.getOldValue());
            m.put("newValue", l.getNewValue());
            m.put("createdAt", l.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
