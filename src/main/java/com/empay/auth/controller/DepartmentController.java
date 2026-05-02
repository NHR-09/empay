package com.empay.auth.controller;

import com.empay.auth.model.Department;
import com.empay.auth.model.User;
import com.empay.auth.repository.DepartmentRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentController(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        List<Map<String, String>> depts = departmentRepository.findByOrganization(userOpt.get().getOrganization())
            .stream().map(d -> Map.of(
                "id", d.getId().toString(),
                "name", d.getDepartmentName(),
                "description", d.getDescription() != null ? d.getDescription() : ""
            )).collect(Collectors.toList());
        return ResponseEntity.ok(depts);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findByEmail(body.get("email"));
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User user = userOpt.get();
        String role = user.getRole().getRoleName();
        if (!List.of("ADMIN", "HR_OFFICER").contains(role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }

        Department dept = new Department();
        dept.setOrganization(user.getOrganization());
        dept.setDepartmentName(body.get("name"));
        dept.setDescription(body.getOrDefault("description", ""));
        departmentRepository.save(dept);

        return ResponseEntity.ok(Map.of("message", "Department created.", "id", dept.getId().toString()));
    }
}
