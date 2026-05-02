package com.empay.auth.controller;

import com.empay.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        try {
            // companyCode defaults to EMPAY001 (the seeded org) if not provided
            String companyCode = body.getOrDefault("companyCode", "EMPAY001");
            userService.registerUser(
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.getOrDefault("phone", ""),
                companyCode,
                body.getOrDefault("role", "EMPLOYEE")
            );
            return ResponseEntity.ok(Map.of("message", "User created and temporary password sent to " + body.get("email")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
