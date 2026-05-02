package com.empay.auth.controller;

import com.empay.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final com.empay.auth.repository.UserRepository userRepository;

    public AdminController(UserService userService, com.empay.auth.repository.UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String requestedBy = body.get("requestedBy");
        if (requestedBy != null) {
            userRepository.findByEmail(requestedBy).ifPresent(u -> {
                String role = u.getRole().getRoleName();
                if (role.equals("EMPLOYEE")) {
                    throw new RuntimeException("Access denied: Employees cannot create users.");
                }
            });
        }
        try {
            String companyCode = body.getOrDefault("companyCode", "OI");
            com.empay.auth.model.User user = userService.registerUser(
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.getOrDefault("phone", ""),
                companyCode,
                body.getOrDefault("role", "EMPLOYEE")
            );
            return ResponseEntity.ok(Map.of(
                "message", "User created successfully.",
                "loginId", user.getLoginId(),
                "email",   user.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
