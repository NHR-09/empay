package com.empay.auth.controller;

import com.empay.auth.model.Employee;
import com.empay.auth.model.LeaveRequest;
import com.empay.auth.model.User;
import com.empay.auth.repository.EmployeeRepository;
import com.empay.auth.repository.LeaveRequestRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public LeaveController(LeaveRequestRepository leaveRepository,
                           EmployeeRepository employeeRepository,
                           UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findByEmail(body.get("email"));
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Employee record not found."));

        LeaveRequest req = new LeaveRequest();
        req.setEmployee(empOpt.get());
        req.setLeaveType(body.getOrDefault("leaveType", "CASUAL"));
        req.setStartDate(LocalDate.parse(body.get("startDate")));
        req.setEndDate(LocalDate.parse(body.get("endDate")));
        req.setReason(body.get("reason"));
        leaveRepository.save(req);
        return ResponseEntity.ok(Map.of("message", "Leave request submitted.", "id", req.getId().toString()));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myLeaves(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.ok(new ArrayList<>());

        return ResponseEntity.ok(toList(leaveRepository.findByEmployeeOrderByCreatedAtDesc(empOpt.get())));
    }

    @GetMapping("/all")
    public ResponseEntity<?> allLeaves(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        return ResponseEntity.ok(toList(leaveRepository.findByOrgId(userOpt.get().getOrganization().getId())));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Optional<LeaveRequest> reqOpt = leaveRepository.findById(id);
        if (reqOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Leave request not found."));

        LeaveRequest req = reqOpt.get();
        String newStatus = body.get("status");
        if (!List.of("APPROVED", "REJECTED", "CANCELLED").contains(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status."));
        }
        req.setStatus(newStatus);
        userRepository.findByEmail(body.getOrDefault("approverEmail", "")).ifPresent(u -> {
            req.setApprovedBy(u);
            req.setApprovedAt(LocalDateTime.now());
        });
        leaveRepository.save(req);
        return ResponseEntity.ok(Map.of("message", "Leave status updated to " + newStatus));
    }

    private List<Map<String, Object>> toList(List<LeaveRequest> requests) {
        return requests.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId().toString());
            m.put("employeeName", r.getEmployee().getUser().getFirstName() + " " + r.getEmployee().getUser().getLastName());
            m.put("employeeCode", r.getEmployee().getEmployeeCode());
            m.put("leaveType", r.getLeaveType());
            m.put("startDate", r.getStartDate().toString());
            m.put("endDate", r.getEndDate().toString());
            m.put("reason", r.getReason() != null ? r.getReason() : "");
            m.put("status", r.getStatus());
            m.put("appliedAt", r.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());
    }
}
