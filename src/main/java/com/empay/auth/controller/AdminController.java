package com.empay.auth.controller;

import com.empay.auth.model.Employee;
import com.empay.auth.model.User;
import com.empay.auth.repository.EmployeeRepository;
import com.empay.auth.repository.UserRepository;
import com.empay.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public AdminController(UserService userService, UserRepository userRepository,
                           EmployeeRepository employeeRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @PatchMapping("/users/{loginId}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable String loginId, @RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findAll().stream()
            .filter(u -> loginId.equals(u.getLoginId()))
            .findFirst();
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User u = userOpt.get();
        String newStatus = body.get("status");
        boolean isActive = "ACTIVE".equalsIgnoreCase(newStatus);
        u.setActive(isActive);
        userRepository.save(u);

        // Also sync the Employee record's status field
        employeeRepository.findByUser(u).ifPresent(emp -> {
            emp.setStatus(isActive ? "ACTIVE" : "INACTIVE");
            employeeRepository.save(emp);
        });

        return ResponseEntity.ok(Map.of("message", "Status updated.", "status", newStatus));
    }

    @DeleteMapping("/users/{loginId}")
    public ResponseEntity<?> deleteUser(@PathVariable String loginId) {
        Optional<User> userOpt = userRepository.findAll().stream()
            .filter(u -> loginId.equals(u.getLoginId()))
            .findFirst();
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        userRepository.delete(userOpt.get());
        return ResponseEntity.ok(Map.of("message", "User deleted."));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<Map<String, String>> users = userRepository.findAll().stream()
            .filter(u -> u.getLoginId() != null && !u.getLoginId().isBlank())
            .map(u -> Map.of(
                "loginId",   u.getLoginId(),
                "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                "lastName",  u.getLastName()  != null ? u.getLastName()  : "",
                "email",     u.getEmail()     != null ? u.getEmail()     : "",
                "role",      u.getRole()      != null ? u.getRole().getRoleName() : "",
                "status",    u.isActive() ? "ACTIVE" : "INACTIVE"
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        List<User> badUsers = userRepository.findAll().stream()
            .filter(u -> u.getLoginId() == null || u.getLoginId().isBlank())
            .collect(Collectors.toList());
        int fixed = 0, deleted = 0;
        for (User u : badUsers) {
            // Don't delete admins/officers - fix their loginId instead
            String role = u.getRole() != null ? u.getRole().getRoleName() : "";
            if (List.of("ADMIN", "HR_OFFICER", "PAYROLL_OFFICER").contains(role)) {
                u.setLoginId("SYS" + role.substring(0,3) + u.getId().toString().substring(0,8).toUpperCase());
                userRepository.save(u);
                fixed++;
            } else {
                employeeRepository.findByUser(u).ifPresent(employeeRepository::delete);
                userRepository.delete(u);
                deleted++;
            }
        }
        return ResponseEntity.ok(Map.of("message", "Fixed " + fixed + ", deleted " + deleted + " user(s)."));
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
            User user = userService.registerUser(
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.getOrDefault("phone", ""),
                companyCode,
                body.getOrDefault("role", "EMPLOYEE")
            );
            // Auto-create employee record
            Employee emp;
            if (employeeRepository.findByUser(user).isEmpty()) {
                emp = new Employee();
                emp.setUser(user);
                emp.setOrganization(user.getOrganization());
                emp.setEmployeeCode(user.getLoginId());
                emp.setDesignation(body.getOrDefault("designation", ""));
                emp.setJoiningDate(LocalDate.now());
            } else {
                emp = employeeRepository.findByUser(user).get();
            }
            // Assign HR manager if provided
            String hrManagerLoginId = body.get("hrManagerLoginId");
            if (hrManagerLoginId != null && !hrManagerLoginId.isBlank()) {
                userRepository.findAll().stream()
                    .filter(u -> hrManagerLoginId.equals(u.getLoginId()))
                    .findFirst()
                    .ifPresent(emp::setHrManager);
            }
            employeeRepository.save(emp);
            return ResponseEntity.ok(Map.of(
                "message", "User created successfully.",
                "loginId", user.getLoginId(),
                "email",   user.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/hr-managers")
    public ResponseEntity<?> getHrManagers() {
        List<Map<String, String>> hrs = userRepository.findAll().stream()
            .filter(u -> u.getRole() != null && "HR_OFFICER".equals(u.getRole().getRoleName()) && u.isActive())
            .map(u -> Map.of(
                "loginId",   u.getLoginId() != null ? u.getLoginId() : "",
                "name",      u.getFirstName() + " " + u.getLastName(),
                "email",     u.getEmail()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(hrs);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User user = userOpt.get();
        long total = userRepository.findAll().stream()
            .filter(u -> u.getOrganization().getId().equals(user.getOrganization().getId())).count();
        long active = userRepository.findAll().stream()
            .filter(u -> u.getOrganization().getId().equals(user.getOrganization().getId()) && u.isActive()).count();

        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("totalEmployees", total);
        statsMap.put("activeEmployees", active);
        return ResponseEntity.ok(statsMap);
    }
}
