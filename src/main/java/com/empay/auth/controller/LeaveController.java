package com.empay.auth.controller;

import com.empay.auth.model.*;
import com.empay.auth.repository.*;
import com.empay.auth.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AuditService auditService;

    public LeaveController(LeaveRequestRepository leaveRepository,
                           EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           LeaveBalanceRepository leaveBalanceRepository,
                           AuditService auditService) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.auditService = auditService;
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findByEmail(body.get("email"));
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        User targetUser = userOpt.get();

        // Auto-create employee record if missing (safety net)
        Employee emp = employeeRepository.findByUser(targetUser).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(targetUser);
            e.setOrganization(targetUser.getOrganization());
            e.setEmployeeCode(targetUser.getLoginId() != null ? targetUser.getLoginId() : "EMP-" + targetUser.getId().toString().substring(0, 8));
            e.setJoiningDate(LocalDate.now());
            return employeeRepository.save(e);
        });

        LeaveRequest req = new LeaveRequest();
        req.setEmployee(emp);
        req.setLeaveType(body.getOrDefault("leaveType", "CASUAL"));
        req.setStartDate(LocalDate.parse(body.get("startDate")));
        req.setEndDate(LocalDate.parse(body.get("endDate")));
        req.setReason(body.get("reason"));
        leaveRepository.save(req);

        auditService.log(targetUser, "APPLY_LEAVE", "LEAVE", null,
            req.getLeaveType() + ": " + req.getStartDate() + " to " + req.getEndDate());

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

    @GetMapping("/balance")
    public ResponseEntity<?> balance(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.ok(new ArrayList<>());

        int year = LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeAndYear(empOpt.get(), year);

        // Auto-initialize leave balances if none exist
        if (balances.isEmpty()) {
            balances = initializeLeaveBalance(empOpt.get(), year);
        }

        List<Map<String, Object>> result = balances.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("leaveType", b.getLeaveType());
            m.put("totalDays", b.getTotalDays());
            m.put("usedDays", b.getUsedDays());
            m.put("remainingDays", b.getRemainingDays());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
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
        String oldStatus = req.getStatus();
        req.setStatus(newStatus);
        userRepository.findByEmail(body.getOrDefault("approverEmail", "")).ifPresent(u -> {
            req.setApprovedBy(u);
            req.setApprovedAt(LocalDateTime.now());
        });
        leaveRepository.save(req);

        // Update leave balance on approval
        if ("APPROVED".equals(newStatus)) {
            long days = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
            int year = req.getStartDate().getYear();
            Employee emp = req.getEmployee();
            String leaveType = req.getLeaveType();

            Optional<LeaveBalance> balOpt = leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(emp, leaveType, year);
            LeaveBalance bal;
            if (balOpt.isPresent()) {
                bal = balOpt.get();
            } else {
                List<LeaveBalance> all = leaveBalanceRepository.findByEmployeeAndYear(emp, year);
                if (all.isEmpty()) initializeLeaveBalance(emp, year);
                bal = leaveBalanceRepository.findByEmployeeAndLeaveTypeAndYear(emp, leaveType, year)
                    .orElseGet(() -> {
                        LeaveBalance nb = new LeaveBalance();
                        nb.setEmployee(emp);
                        nb.setLeaveType(leaveType);
                        nb.setTotalDays(12);
                        nb.setUsedDays(0);
                        nb.setRemainingDays(12);
                        nb.setYear(year);
                        return leaveBalanceRepository.save(nb);
                    });
            }
            bal.setUsedDays(bal.getUsedDays() + (int) days);
            bal.setRemainingDays(bal.getTotalDays() - bal.getUsedDays());
            leaveBalanceRepository.save(bal);
        }

        // Audit + Notification
        auditService.log(null, "UPDATE_LEAVE_STATUS", "LEAVE", oldStatus, newStatus);
        auditService.notify(req.getEmployee().getUser(),
            "Your " + req.getLeaveType() + " leave request has been " + newStatus.toLowerCase() + ".", "LEAVE");

        return ResponseEntity.ok(Map.of("message", "Leave status updated to " + newStatus));
    }

    private List<LeaveBalance> initializeLeaveBalance(Employee emp, int year) {
        List<LeaveBalance> result = new ArrayList<>();
        Map<String, Integer> defaults = Map.of("CASUAL", 12, "SICK", 10, "EARNED", 15);
        for (Map.Entry<String, Integer> e : defaults.entrySet()) {
            LeaveBalance b = new LeaveBalance();
            b.setEmployee(emp);
            b.setLeaveType(e.getKey());
            b.setTotalDays(e.getValue());
            b.setUsedDays(0);
            b.setRemainingDays(e.getValue());
            b.setYear(year);
            leaveBalanceRepository.save(b);
            result.add(b);
        }
        return result;
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
