package com.empay.auth.controller;

import com.empay.auth.model.Attendance;
import com.empay.auth.model.Employee;
import com.empay.auth.model.User;
import com.empay.auth.repository.AttendanceRepository;
import com.empay.auth.repository.EmployeeRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public AttendanceController(AttendanceRepository attendanceRepository,
                                EmployeeRepository employeeRepository,
                                UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findByEmail(body.get("email"));
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Employee record not found. Please contact HR."));

        Employee emp = empOpt.get();
        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByEmployeeAndAttendanceDate(emp, today).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Already checked in today."));
        }

        Attendance att = new Attendance();
        att.setEmployee(emp);
        att.setOrganization(emp.getOrganization());
        att.setAttendanceDate(today);
        att.setCheckIn(LocalDateTime.now());
        att.setStatus("PRESENT");
        attendanceRepository.save(att);
        return ResponseEntity.ok(Map.of("message", "Checked in successfully.", "checkIn", att.getCheckIn().toString()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = userRepository.findByEmail(body.get("email"));
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Employee record not found."));

        Employee emp = empOpt.get();
        LocalDate today = LocalDate.now();
        Optional<Attendance> attOpt = attendanceRepository.findByEmployeeAndAttendanceDate(emp, today);
        if (attOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "No check-in found for today."));

        Attendance att = attOpt.get();
        if (att.getCheckOut() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Already checked out today."));
        }
        att.setCheckOut(LocalDateTime.now());
        if (att.getCheckIn() != null) {
            long minutes = java.time.Duration.between(att.getCheckIn(), att.getCheckOut()).toMinutes();
            att.setTotalHours(new java.math.BigDecimal(minutes).divide(new java.math.BigDecimal(60), 2, java.math.RoundingMode.HALF_UP));
        }
        attendanceRepository.save(att);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Checked out successfully.");
        resp.put("totalHours", att.getTotalHours() != null ? att.getTotalHours().toString() : "0");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/my")
    public ResponseEntity<?> myAttendance(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.ok(new ArrayList<>());

        List<Map<String, Object>> list = attendanceRepository
            .findByEmployeeOrderByAttendanceDateDesc(empOpt.get())
            .stream().limit(60)
            .map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("date", a.getAttendanceDate().toString());
                m.put("checkIn", a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                m.put("checkOut", a.getCheckOut() != null ? a.getCheckOut().toString() : "");
                m.put("totalHours", a.getTotalHours() != null ? a.getTotalHours() : 0);
                m.put("status", a.getStatus());
                return m;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/today-status")
    public ResponseEntity<?> todayStatus(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        Map<String, Object> m = new HashMap<>();
        if (empOpt.isEmpty()) {
            m.put("checkedIn", false);
            m.put("checkedOut", false);
            m.put("checkIn", "");
            m.put("checkOut", "");
            return ResponseEntity.ok(m);
        }

        Optional<Attendance> att = attendanceRepository.findByEmployeeAndAttendanceDate(empOpt.get(), LocalDate.now());
        m.put("checkedIn", att.isPresent());
        m.put("checkedOut", att.map(a -> a.getCheckOut() != null).orElse(false));
        m.put("checkIn", att.map(a -> a.getCheckIn() != null ? a.getCheckIn().toString() : "").orElse(""));
        m.put("checkOut", att.map(a -> a.getCheckOut() != null ? a.getCheckOut().toString() : "").orElse(""));
        return ResponseEntity.ok(m);
    }

    @GetMapping("/all")
    public ResponseEntity<?> allAttendance(@RequestParam String email, @RequestParam int month, @RequestParam int year) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        List<Map<String, Object>> list = attendanceRepository
            .findByOrgAndMonthYear(userOpt.get().getOrganization().getId(), month, year)
            .stream()
            .map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("employeeName", a.getEmployee().getUser().getFirstName() + " " + a.getEmployee().getUser().getLastName());
                m.put("employeeCode", a.getEmployee().getEmployeeCode());
                m.put("date", a.getAttendanceDate().toString());
                m.put("checkIn", a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                m.put("checkOut", a.getCheckOut() != null ? a.getCheckOut().toString() : "");
                m.put("totalHours", a.getTotalHours() != null ? a.getTotalHours() : 0);
                m.put("status", a.getStatus());
                return m;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
