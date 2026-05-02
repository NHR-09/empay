package com.empay.auth.controller;

import com.empay.auth.model.Employee;
import com.empay.auth.model.Payroll;
import com.empay.auth.model.User;
import com.empay.auth.repository.AttendanceRepository;
import com.empay.auth.repository.EmployeeRepository;
import com.empay.auth.repository.LeaveRequestRepository;
import com.empay.auth.repository.PayrollRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;

    public PayrollController(PayrollRepository payrollRepository, EmployeeRepository employeeRepository,
                             UserRepository userRepository, AttendanceRepository attendanceRepository,
                             LeaveRequestRepository leaveRepository) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> body) {
        String generatorEmail = (String) body.get("generatorEmail");
        String targetEmail = (String) body.get("employeeEmail");
        int month = Integer.parseInt(body.get("month").toString());
        int year = Integer.parseInt(body.get("year").toString());

        Optional<User> generatorOpt = userRepository.findByEmail(generatorEmail);
        if (generatorOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Generator user not found."));

        User generator = generatorOpt.get();
        String role = generator.getRole().getRoleName();
        if (!List.of("ADMIN", "PAYROLL_OFFICER").contains(role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }

        Optional<User> targetOpt = userRepository.findByEmail(targetEmail);
        if (targetOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Target employee not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(targetOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Employee record not found."));

        Employee emp = empOpt.get();
        if (payrollRepository.findByEmployeeAndPayMonthAndPayYear(emp, month, year).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Payroll already generated for this period."));
        }

        Payroll p = calculatePayroll(emp, month, year, generator);
        payrollRepository.save(p);
        return ResponseEntity.ok(toMap(p));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myPayroll(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        Optional<Employee> empOpt = employeeRepository.findByUser(userOpt.get());
        if (empOpt.isEmpty()) return ResponseEntity.ok(new ArrayList<>());

        List<Map<String, Object>> list = payrollRepository.findByEmployeeOrderByPayYearDescPayMonthDesc(empOpt.get())
            .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<?> allPayroll(@RequestParam String email, @RequestParam int month, @RequestParam int year) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));

        List<Map<String, Object>> list = payrollRepository
            .findByOrgAndMonthYear(userOpt.get().getOrganization().getId(), month, year)
            .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Optional<Payroll> pOpt = payrollRepository.findById(id);
        if (pOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Payroll not found."));

        Payroll p = pOpt.get();
        p.setPayrollStatus(body.get("status"));
        payrollRepository.save(p);
        return ResponseEntity.ok(Map.of("message", "Payroll status updated."));
    }

    private Payroll calculatePayroll(Employee emp, int month, int year, User generator) {
        BigDecimal basic = emp.getBasicSalary() != null ? emp.getBasicSalary() : BigDecimal.ZERO;
        int totalDays = 26;
        long presentDays = attendanceRepository.countPresentDays(emp, month, year);
        long approvedLeaves = leaveRepository.countApprovedLeaves(emp, month, year);

        // HRA = 40% of basic, Bonus = 10% of basic
        BigDecimal hra = basic.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = basic.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

        // Attendance-based salary: (basic / totalDays) * (presentDays + approvedLeaves)
        BigDecimal effectiveDays = new BigDecimal(presentDays + approvedLeaves);
        BigDecimal earnedBasic = presentDays > 0
            ? basic.multiply(effectiveDays).divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP)
            : basic; // if no attendance data, use full basic
        BigDecimal earnedHra = hra.multiply(effectiveDays).divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP);

        BigDecimal gross = earnedBasic.add(earnedHra).add(bonus);

        // PF = 12% of basic, Professional Tax = flat 200
        BigDecimal pf = basic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profTax = new BigDecimal("200.00");
        BigDecimal totalDeductions = pf.add(profTax);
        BigDecimal net = gross.subtract(totalDeductions);

        Payroll p = new Payroll();
        p.setEmployee(emp);
        p.setOrganization(emp.getOrganization());
        p.setPayMonth(month);
        p.setPayYear(year);
        p.setTotalWorkingDays(totalDays);
        p.setPresentDays((int) presentDays);
        p.setLeavesTaken((int) approvedLeaves);
        p.setBasicSalary(earnedBasic);
        p.setHra(earnedHra);
        p.setBonus(bonus);
        p.setGrossSalary(gross);
        p.setPfDeduction(pf);
        p.setProfessionalTax(profTax);
        p.setTotalDeductions(totalDeductions);
        p.setNetSalary(net);
        p.setGeneratedBy(generator);
        return p;
    }

    private Map<String, Object> toMap(Payroll p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId().toString());
        m.put("employeeName", p.getEmployee().getUser().getFirstName() + " " + p.getEmployee().getUser().getLastName());
        m.put("employeeCode", p.getEmployee().getEmployeeCode());
        m.put("email", p.getEmployee().getUser().getEmail());
        m.put("payMonth", p.getPayMonth());
        m.put("payYear", p.getPayYear());
        m.put("totalWorkingDays", p.getTotalWorkingDays());
        m.put("presentDays", p.getPresentDays());
        m.put("leavesTaken", p.getLeavesTaken());
        m.put("basicSalary", p.getBasicSalary());
        m.put("hra", p.getHra());
        m.put("bonus", p.getBonus());
        m.put("grossSalary", p.getGrossSalary());
        m.put("pfDeduction", p.getPfDeduction());
        m.put("professionalTax", p.getProfessionalTax());
        m.put("totalDeductions", p.getTotalDeductions());
        m.put("netSalary", p.getNetSalary());
        m.put("status", p.getPayrollStatus());
        m.put("generatedAt", p.getGeneratedAt().toString());
        return m;
    }
}
