package com.empay.auth.controller;

import com.empay.auth.model.Employee;
import com.empay.auth.model.User;
import com.empay.auth.repository.EmployeeRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeController(EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User user = userOpt.get();
        Optional<Employee> empOpt = employeeRepository.findByUser(user);
        Map<String, Object> profile = new HashMap<>();
        profile.put("loginId", user.getLoginId() != null ? user.getLoginId() : "");
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone() != null ? user.getPhone() : "");
        profile.put("role", user.getRole().getRoleName());
        empOpt.ifPresent(emp -> {
            profile.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "");
            profile.put("joiningDate", emp.getJoiningDate().toString());
            profile.put("employmentType", emp.getEmploymentType());
            profile.put("basicSalary", emp.getBasicSalary());
            profile.put("bankAccountNo", emp.getBankAccountNo() != null ? emp.getBankAccountNo() : "");
            profile.put("panNumber", emp.getPanNumber() != null ? emp.getPanNumber() : "");
            profile.put("employeeCode", emp.getEmployeeCode());
        });
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User user = userOpt.get();
        if (body.containsKey("firstName")) user.setFirstName(body.get("firstName"));
        if (body.containsKey("lastName")) user.setLastName(body.get("lastName"));
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        userRepository.save(user);

        employeeRepository.findByUser(user).ifPresent(emp -> {
            if (body.containsKey("designation")) emp.setDesignation(body.get("designation"));
            if (body.containsKey("bankAccountNo")) emp.setBankAccountNo(body.get("bankAccountNo"));
            if (body.containsKey("panNumber")) emp.setPanNumber(body.get("panNumber"));
            if (body.containsKey("basicSalary") && !body.get("basicSalary").isBlank()) {
                emp.setBasicSalary(new BigDecimal(body.get("basicSalary")));
            }
            employeeRepository.save(emp);
        });
        return ResponseEntity.ok(Map.of("message", "Profile updated."));
    }

    @GetMapping
    public ResponseEntity<?> listEmployees(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        List<Map<String, Object>> result = employeeRepository
            .findByOrganizationId(userOpt.get().getOrganization().getId())
            .stream()
            .map(emp -> {
                Map<String, Object> m = new HashMap<>();
                m.put("employeeCode", emp.getEmployeeCode());
                m.put("firstName", emp.getUser().getFirstName());
                m.put("lastName", emp.getUser().getLastName());
                m.put("email", emp.getUser().getEmail());
                m.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "");
                m.put("employmentType", emp.getEmploymentType());
                m.put("basicSalary", emp.getBasicSalary());
                m.put("joiningDate", emp.getJoiningDate().toString());
                m.put("status", emp.getStatus());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ensure")
    public ResponseEntity<?> ensureEmployeeRecord(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User user = userOpt.get();
        if (employeeRepository.findByUser(user).isPresent()) {
            return ResponseEntity.ok(Map.of("message", "Already exists."));
        }
        Employee emp = new Employee();
        emp.setUser(user);
        emp.setOrganization(user.getOrganization());
        emp.setEmployeeCode(user.getLoginId() != null ? user.getLoginId() : "EMP-" + user.getId().toString().substring(0, 8));
        emp.setJoiningDate(LocalDate.now());
        employeeRepository.save(emp);
        return ResponseEntity.ok(Map.of("message", "Employee record created."));
    }
}
