package com.empay.auth;

import com.empay.auth.model.*;
import com.empay.auth.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Seeds realistic demo data spanning May 2025 → May 2026 in IST timezone.
 * Only runs when no employees exist (safe to restart without duplication).
 */
@Component
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayrollRepository payrollRepository;
    private final DeductionRepository deductionRepository;
    private final PayslipRepository payslipRepository;
    private final PasswordEncoder passwordEncoder;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final Random rng = new Random(42); // deterministic for consistent demo

    public DataSeeder(UserRepository userRepository, EmployeeRepository employeeRepository,
                      OrganizationRepository organizationRepository, RoleRepository roleRepository,
                      AttendanceRepository attendanceRepository, LeaveRequestRepository leaveRequestRepository,
                      LeaveBalanceRepository leaveBalanceRepository, PayrollRepository payrollRepository,
                      DeductionRepository deductionRepository, PayslipRepository payslipRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.payrollRepository = payrollRepository;
        this.deductionRepository = deductionRepository;
        this.payslipRepository = payslipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() > 0) {
            System.out.println("⏭ Demo data already exists — skipping seeder.");
            return;
        }

        Organization org = organizationRepository.findByCompanyCode("OI")
                .orElseThrow(() -> new RuntimeException("Organization OI not found. Ensure admin seeder runs first."));

        // Fetch the admin user to generate payroll
        User admin = userRepository.findByEmail("admin@odooindia.com").orElse(null);

        System.out.println("🌱 Seeding demo data (May 2025 → May 2026, IST)...");

        // Create demo employees
        List<DemoUser> demoUsers = List.of(
            new DemoUser("Priya", "Sharma", "priya.sharma@odooindia.com", "HR_OFFICER", "HR Manager", new BigDecimal("55000.00")),
            new DemoUser("Rahul", "Verma", "rahul.verma@odooindia.com", "PAYROLL_OFFICER", "Payroll Specialist", new BigDecimal("48000.00")),
            new DemoUser("Ananya", "Gupta", "ananya.gupta@odooindia.com", "EMPLOYEE", "Software Engineer", new BigDecimal("62000.00")),
            new DemoUser("Vikram", "Patel", "vikram.patel@odooindia.com", "EMPLOYEE", "QA Analyst", new BigDecimal("42000.00"))
        );

        List<Employee> employees = new ArrayList<>();
        int serial = 2; // 1 is admin
        for (DemoUser du : demoUsers) {
            if (userRepository.findByEmail(du.email).isPresent()) continue;

            RoleEntity role = roleRepository.findByRoleName(du.role)
                    .orElseThrow(() -> new RuntimeException("Role " + du.role + " not found."));

            String loginId = "OI" + du.firstName.substring(0, 2).toUpperCase()
                    + du.lastName.substring(0, 2).toUpperCase() + "2025" + String.format("%04d", serial++);

            User user = new User();
            user.setFirstName(du.firstName);
            user.setLastName(du.lastName);
            user.setEmail(du.email);
            user.setPhone("+91-" + (9000000000L + rng.nextInt(999999999)));
            user.setPassword(passwordEncoder.encode("Demo@123"));
            user.setOrganization(org);
            user.setRole(role);
            user.setLoginId(loginId);
            user.setMustChangePassword(false);
            userRepository.save(user);

            Employee emp = new Employee();
            emp.setUser(user);
            emp.setOrganization(org);
            emp.setEmployeeCode(loginId);
            emp.setDesignation(du.designation);
            emp.setJoiningDate(LocalDate.of(2025, 1, 15).plusDays(rng.nextInt(60)));
            emp.setBasicSalary(du.salary);
            emp.setBankAccountNo("XXXX" + String.format("%04d", 1000 + rng.nextInt(9000)));
            emp.setPanNumber("ABCDE" + (1000 + rng.nextInt(9000)) + "F");
            employeeRepository.save(emp);
            employees.add(emp);

            System.out.println("   👤 " + du.firstName + " " + du.lastName + " (" + du.role + ") — " + loginId);
        }

        // Also create employee record for admin if missing
        if (admin != null && employeeRepository.findByUser(admin).isEmpty()) {
            Employee adminEmp = new Employee();
            adminEmp.setUser(admin);
            adminEmp.setOrganization(org);
            adminEmp.setEmployeeCode(admin.getLoginId());
            adminEmp.setDesignation("System Administrator");
            adminEmp.setJoiningDate(LocalDate.of(2025, 1, 1));
            adminEmp.setBasicSalary(new BigDecimal("75000.00"));
            employeeRepository.save(adminEmp);
            employees.add(adminEmp);
        }

        // Seed data from May 2025 to current month
        LocalDate seedStart = LocalDate.of(2025, 5, 1);
        LocalDate seedEnd = LocalDate.now(IST);

        for (Employee emp : employees) {
            seedAttendance(emp, seedStart, seedEnd);
            seedLeaveRequests(emp, seedStart, seedEnd, admin);
            seedLeaveBalances(emp);
        }

        // Seed payroll for completed months (May 2025 → previous month)
        YearMonth startMonth = YearMonth.of(2025, 5);
        YearMonth endMonth = YearMonth.from(seedEnd).minusMonths(1);
        for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
            for (Employee emp : employees) {
                seedPayroll(emp, ym.getMonthValue(), ym.getYear(), admin);
            }
        }

        System.out.println("✅ Demo data seeded successfully! (IST timezone, May 2025 → " + seedEnd + ")");
    }

    private void seedAttendance(Employee emp, LocalDate start, LocalDate end) {
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            // Skip ~10% of days randomly (leaves / absences)
            if (rng.nextDouble() < 0.10) continue;

            int checkInHour = 9 + rng.nextInt(2);   // 9-10 AM
            int checkInMin = rng.nextInt(45);
            LocalDateTime checkIn = LocalDateTime.of(d, LocalTime.of(checkInHour, checkInMin));

            int workHours = 7 + rng.nextInt(3);     // 7-9 hours
            LocalDateTime checkOut = checkIn.plusHours(workHours).plusMinutes(rng.nextInt(30));

            long totalMinutes = Duration.between(checkIn, checkOut).toMinutes();

            Attendance att = new Attendance();
            att.setEmployee(emp);
            att.setOrganization(emp.getOrganization());
            att.setAttendanceDate(d);
            att.setCheckIn(checkIn);
            att.setCheckOut(checkOut);
            att.setTotalHours(new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP));
            att.setStatus("PRESENT");
            attendanceRepository.save(att);
        }
    }

    private void seedLeaveRequests(Employee emp, LocalDate start, LocalDate end, User approver) {
        String[] types = {"CASUAL", "SICK", "EARNED"};
        String[] statuses = {"APPROVED", "APPROVED", "APPROVED", "REJECTED", "PENDING"};

        int leaveCount = 4 + rng.nextInt(6); // 4-9 leave requests per employee
        for (int i = 0; i < leaveCount; i++) {
            long dayRange = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            if (dayRange <= 5) break;
            LocalDate leaveStart = start.plusDays(rng.nextInt((int) dayRange - 3));
            int leaveDays = 1 + rng.nextInt(3); // 1-3 day leaves
            LocalDate leaveEnd = leaveStart.plusDays(leaveDays - 1);

            // Don't create future pending leaves beyond today
            String status = statuses[rng.nextInt(statuses.length)];
            if (leaveStart.isAfter(LocalDate.now(IST))) {
                status = "PENDING";
            }

            LeaveRequest req = new LeaveRequest();
            req.setEmployee(emp);
            req.setLeaveType(types[rng.nextInt(types.length)]);
            req.setStartDate(leaveStart);
            req.setEndDate(leaveEnd);
            req.setReason(getRandomReason(req.getLeaveType()));
            req.setStatus(status);
            if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
                req.setApprovedBy(approver);
                req.setApprovedAt(leaveStart.minusDays(1).atTime(14, 30));
            }
            leaveRequestRepository.save(req);
        }
    }

    private void seedLeaveBalances(Employee emp) {
        for (int year : new int[]{2025, 2026}) {
            if (leaveBalanceRepository.findByEmployeeAndYear(emp, year).isEmpty()) {
                Map<String, Integer> defaults = Map.of("CASUAL", 12, "SICK", 10, "EARNED", 15);
                for (Map.Entry<String, Integer> e : defaults.entrySet()) {
                    int used = rng.nextInt(Math.min(e.getValue(), year == 2025 ? 8 : 3));
                    LeaveBalance lb = new LeaveBalance();
                    lb.setEmployee(emp);
                    lb.setLeaveType(e.getKey());
                    lb.setTotalDays(e.getValue());
                    lb.setUsedDays(used);
                    lb.setRemainingDays(e.getValue() - used);
                    lb.setYear(year);
                    leaveBalanceRepository.save(lb);
                }
            }
        }
    }

    private void seedPayroll(Employee emp, int month, int year, User generator) {
        if (payrollRepository.findByEmployeeAndPayMonthAndPayYear(emp, month, year).isPresent()) return;

        BigDecimal basic = emp.getBasicSalary() != null ? emp.getBasicSalary() : new BigDecimal("25000.00");
        int totalDays = 26;
        long presentDays = attendanceRepository.countPresentDays(emp, month, year);
        long approvedLeaves = leaveRequestRepository.countApprovedLeaves(emp, month, year);

        boolean noAttendanceData = (presentDays == 0 && approvedLeaves == 0);
        BigDecimal earnedBasic;
        BigDecimal hra = basic.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = basic.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal earnedHra;

        if (noAttendanceData) {
            earnedBasic = basic;
            earnedHra = hra;
        } else {
            BigDecimal effectiveDays = new BigDecimal(presentDays + approvedLeaves);
            earnedBasic = basic.multiply(effectiveDays).divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP);
            earnedHra = hra.multiply(effectiveDays).divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP);
        }

        BigDecimal gross = earnedBasic.add(earnedHra).add(bonus);
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
        p.setPayrollStatus(rng.nextDouble() < 0.7 ? "PAID" : "GENERATED");
        p.setGeneratedBy(generator);
        // Set generated timestamp to the month end
        p.setGeneratedAt(LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).atTime(18, 0));
        payrollRepository.save(p);

        // Save deductions
        Deduction pfDed = new Deduction();
        pfDed.setPayroll(p);
        pfDed.setDeductionType("Provident Fund (12%)");
        pfDed.setAmount(pf);
        pfDed.setDescription("Employee PF contribution at 12% of basic salary");
        deductionRepository.save(pfDed);

        Deduction ptDed = new Deduction();
        ptDed.setPayroll(p);
        ptDed.setDeductionType("Professional Tax");
        ptDed.setAmount(profTax);
        ptDed.setDescription("Monthly professional tax");
        deductionRepository.save(ptDed);

        // Create payslip
        PayslipEntity slip = new PayslipEntity();
        slip.setPayroll(p);
        payslipRepository.save(slip);
    }

    private String getRandomReason(String type) {
        String[][] reasons = {
            {"Family function", "Personal work", "Festival celebration", "Moving house", "Bank work"},
            {"Fever and cold", "Doctor appointment", "Medical checkup", "Not feeling well", "Dental procedure"},
            {"Vacation trip", "Annual family visit", "Rest and recharge", "Travel plans", "Home town visit"}
        };
        int idx = "SICK".equals(type) ? 1 : "EARNED".equals(type) ? 2 : 0;
        return reasons[idx][rng.nextInt(reasons[idx].length)];
    }

    private record DemoUser(String firstName, String lastName, String email, String role, String designation, BigDecimal salary) {}
}
