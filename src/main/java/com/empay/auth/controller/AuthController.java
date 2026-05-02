package com.empay.auth.controller;

import com.empay.auth.model.Employee;
import com.empay.auth.model.User;
import com.empay.auth.repository.EmployeeRepository;
import com.empay.auth.repository.UserRepository;
import com.empay.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EmployeeRepository employeeRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          UserService userService, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            User user = userService.registerUser(
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.get("phone"),
                body.get("companyCode"),
                body.getOrDefault("role", "EMPLOYEE")
            );
            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully. Temporary password sent to email.",
                "email", user.getEmail(),
                "loginId", user.getLoginId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty() || !passwordEncoder.matches(password, optUser.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }

        User user = optUser.get();
        if (!user.isActive()) {
            return ResponseEntity.status(403).body(Map.of("message", "Account is inactive. Contact your administrator."));
        }
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Auto-create employee record if missing
        if (employeeRepository.findByUser(user).isEmpty()) {
            Employee emp = new Employee();
            emp.setUser(user);
            emp.setOrganization(user.getOrganization());
            emp.setEmployeeCode(user.getLoginId() != null ? user.getLoginId() : "EMP-" + user.getId().toString().substring(0, 8));
            emp.setJoiningDate(LocalDate.now());
            employeeRepository.save(emp);
        }

        return ResponseEntity.ok(Map.of(
            "message", "Login successful",
            "mustChangePassword", user.isMustChangePassword(),
            "role", user.getRole().getRoleName(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "email", user.getEmail(),
            "loginId", user.getLoginId() != null ? user.getLoginId() : ""
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        try {
            userService.changePassword(body.get("email"), body.get("currentPassword"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
