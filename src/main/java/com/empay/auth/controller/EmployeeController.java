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
        profile.put("profileImage", user.getProfileImage() != null ? user.getProfileImage() : "");
        empOpt.ifPresent(emp -> {
            profile.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "");
            profile.put("joiningDate", emp.getJoiningDate().toString());
            profile.put("employmentType", emp.getEmploymentType());
            profile.put("basicSalary", emp.getBasicSalary());
            profile.put("bankAccountNo", emp.getBankAccountNo() != null ? emp.getBankAccountNo() : "");
            profile.put("panNumber", emp.getPanNumber() != null ? emp.getPanNumber() : "");
            profile.put("aadhaarNumber", emp.getAadhaarNumber() != null ? emp.getAadhaarNumber() : "");
            profile.put("employeeCode", emp.getEmployeeCode());
            profile.put("hrManager", emp.getHrManager() != null ?
                emp.getHrManager().getFirstName() + " " + emp.getHrManager().getLastName() : "");
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
        if (body.containsKey("profileImage")) user.setProfileImage(body.get("profileImage"));
        userRepository.save(user);

        employeeRepository.findByUser(user).ifPresent(emp -> {
            if (body.containsKey("designation")) emp.setDesignation(body.get("designation"));
            if (body.containsKey("bankAccountNo")) emp.setBankAccountNo(body.get("bankAccountNo"));
            if (body.containsKey("panNumber")) emp.setPanNumber(body.get("panNumber"));
            if (body.containsKey("aadhaarNumber")) emp.setAadhaarNumber(body.get("aadhaarNumber"));
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

    /** Returns basic info for a single employee by employeeCode (for the info modal) */
    @GetMapping("/info/{employeeCode}")
    public ResponseEntity<?> getEmployeeInfo(@PathVariable String employeeCode) {
        Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(employeeCode);
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found."));
        Employee emp = empOpt.get();
        User u = emp.getUser();
        Map<String, Object> m = new HashMap<>();
        m.put("employeeCode", emp.getEmployeeCode());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone() != null ? u.getPhone() : "");
        m.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "");
        m.put("joiningDate", emp.getJoiningDate().toString());
        m.put("employmentType", emp.getEmploymentType());
        m.put("status", emp.getStatus());
        m.put("hrManager", emp.getHrManager() != null ?
            emp.getHrManager().getFirstName() + " " + emp.getHrManager().getLastName() : "—");
        return ResponseEntity.ok(m);
    }

    /** HR: list employees assigned to this HR officer */
    @GetMapping("/hr-team")
    public ResponseEntity<?> hrTeam(@RequestParam String email) {
        Optional<User> hrOpt = userRepository.findByEmail(email);
        if (hrOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "User not found."));
        User hr = hrOpt.get();
        if (!"HR_OFFICER".equals(hr.getRole().getRoleName()))
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));

        List<Map<String, Object>> result = employeeRepository.findByHrManager(hr).stream()
            .map(emp -> {
                Map<String, Object> m = new HashMap<>();
                m.put("employeeCode", emp.getEmployeeCode());
                m.put("firstName", emp.getUser().getFirstName());
                m.put("lastName", emp.getUser().getLastName());
                m.put("email", emp.getUser().getEmail());
                m.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "");
                m.put("status", emp.getStatus());
                m.put("bankAccountNo", emp.getBankAccountNo() != null ? emp.getBankAccountNo() : "");
                m.put("panNumber", emp.getPanNumber() != null ? emp.getPanNumber() : "");
                m.put("aadhaarNumber", emp.getAadhaarNumber() != null ? emp.getAadhaarNumber() : "");
                m.put("basicSalary", emp.getBasicSalary());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** HR: set salary details for an employee (requires bank + PAN + Aadhaar filled) */
    @PutMapping("/salary")
    public ResponseEntity<?> setSalary(@RequestBody Map<String, String> body) {
        String hrEmail = body.get("hrEmail");
        String empCode = body.get("employeeCode");

        Optional<User> hrOpt = userRepository.findByEmail(hrEmail);
        if (hrOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "HR user not found."));
        if (!"HR_OFFICER".equals(hrOpt.get().getRole().getRoleName()))
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));

        Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(empCode);
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found."));

        Employee emp = empOpt.get();
        // Constraint: bank, PAN, Aadhaar must be filled
        if (emp.getBankAccountNo() == null || emp.getBankAccountNo().isBlank() ||
            emp.getPanNumber() == null || emp.getPanNumber().isBlank() ||
            emp.getAadhaarNumber() == null || emp.getAadhaarNumber().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "Employee must have Bank Account, PAN, and Aadhaar filled before adding salary details."));
        }

        if (body.containsKey("basicSalary") && !body.get("basicSalary").isBlank())
            emp.setBasicSalary(new BigDecimal(body.get("basicSalary")));
        if (body.containsKey("designation") && !body.get("designation").isBlank())
            emp.setDesignation(body.get("designation"));
        if (body.containsKey("employmentType") && !body.get("employmentType").isBlank())
            emp.setEmploymentType(body.get("employmentType"));
        if (body.containsKey("pfNumber") && !body.get("pfNumber").isBlank())
            emp.setPfNumber(body.get("pfNumber"));

        employeeRepository.save(emp);
        return ResponseEntity.ok(Map.of("message", "Salary details updated."));
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
