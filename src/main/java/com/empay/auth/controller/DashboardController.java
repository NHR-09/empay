package com.empay.auth.controller;

import com.empay.auth.model.User;
import com.empay.auth.repository.UserRepository;
import com.empay.auth.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final UserRepository userRepository;
    
    public DashboardController(DashboardService dashboardService, EmployeeService employeeService,
                             AttendanceService attendanceService, LeaveService leaveService,
                             PayrollService payrollService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.employeeService = employeeService;
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;
        this.userRepository = userRepository;
    }
    
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        User user = userOpt.get();
        Map<String, Object> summary = dashboardService.getSummary(user.getOrganization().getId(), email);
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/employees/cached")
    public ResponseEntity<?> getCachedEmployees(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        List<Map<String, Object>> employees = employeeService.getEmployeeList(userOpt.get().getOrganization().getId());
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/leave/queue")
    public ResponseEntity<?> getLeaveQueue(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Queue<Map<String, Object>> queue = leaveService.getPendingQueue(userOpt.get().getOrganization().getId());
        return ResponseEntity.ok(queue);
    }
    
    @GetMapping("/payroll/report")
    public ResponseEntity<?> getPayrollReport(@RequestParam String email, @RequestParam int month, @RequestParam int year) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Map<String, Object> report = payrollService.getMonthlyReport(userOpt.get().getOrganization().getId(), month, year);
        return ResponseEntity.ok(report);
    }
}
