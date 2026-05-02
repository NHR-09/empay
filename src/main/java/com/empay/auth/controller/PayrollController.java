package com.empay.auth.controller;

import com.empay.auth.model.*;
import com.empay.auth.repository.*;
import com.empay.auth.service.AuditService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final DeductionRepository deductionRepository;
    private final PayslipRepository payslipRepository;
    private final AuditService auditService;

    public PayrollController(PayrollRepository payrollRepository, EmployeeRepository employeeRepository,
                             UserRepository userRepository, AttendanceRepository attendanceRepository,
                             LeaveRequestRepository leaveRepository, DeductionRepository deductionRepository,
                             PayslipRepository payslipRepository, AuditService auditService) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.deductionRepository = deductionRepository;
        this.payslipRepository = payslipRepository;
        this.auditService = auditService;
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
        if (targetOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Target employee not found. Check the email address."));

        User targetUser = targetOpt.get();
        Employee emp = employeeRepository.findByUser(targetUser).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(targetUser);
            e.setOrganization(targetUser.getOrganization());
            e.setEmployeeCode(targetUser.getLoginId() != null ? targetUser.getLoginId() : "EMP-" + targetUser.getId().toString().substring(0, 8));
            e.setJoiningDate(java.time.LocalDate.now());
            return employeeRepository.save(e);
        });

        if (payrollRepository.findByEmployeeAndPayMonthAndPayYear(emp, month, year).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Payroll already generated for this period."));
        }

        Payroll p = calculatePayroll(emp, month, year, generator);
        payrollRepository.save(p);

        // Save individual deduction rows
        Deduction pfDed = new Deduction();
        pfDed.setPayroll(p);
        pfDed.setDeductionType("Provident Fund (12%)");
        pfDed.setAmount(p.getPfDeduction());
        pfDed.setDescription("Employee PF contribution at 12% of basic salary");
        deductionRepository.save(pfDed);

        Deduction ptDed = new Deduction();
        ptDed.setPayroll(p);
        ptDed.setDeductionType("Professional Tax");
        ptDed.setAmount(p.getProfessionalTax());
        ptDed.setDescription("Monthly professional tax");
        deductionRepository.save(ptDed);

        // Create payslip record
        PayslipEntity slip = new PayslipEntity();
        slip.setPayroll(p);
        payslipRepository.save(slip);

        // Audit log + notification
        String monthNames = new String[]{"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"}[month];
        auditService.log(generator, "GENERATE_PAYROLL", "PAYROLL", null,
            targetUser.getFirstName() + " " + targetUser.getLastName() + " - " + monthNames + " " + year);
        auditService.notify(targetUser, "Your payslip for " + monthNames + " " + year + " has been generated. Net pay: ₹" + p.getNetSalary(), "PAYROLL");

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

        String role = userOpt.get().getRole().getRoleName();
        if (!List.of("ADMIN", "PAYROLL_OFFICER").contains(role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }

        List<Map<String, Object>> list = payrollRepository.findByOrganizationAndPayMonthAndPayYear(
                userOpt.get().getOrganization(), month, year)
            .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Optional<Payroll> pOpt = payrollRepository.findById(id);
        if (pOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Payroll not found."));

        Payroll p = pOpt.get();
        String oldStatus = p.getPayrollStatus();
        p.setPayrollStatus(body.get("status"));
        payrollRepository.save(p);

        auditService.log(null, "UPDATE_PAYROLL_STATUS", "PAYROLL", oldStatus, body.get("status"));
        if ("PAID".equals(body.get("status"))) {
            auditService.notify(p.getEmployee().getUser(),
                "Your salary for " + p.getPayMonth() + "/" + p.getPayYear() + " has been marked as PAID.", "PAYROLL");
        }
        return ResponseEntity.ok(Map.of("message", "Payroll status updated."));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable UUID id) {
        Optional<Payroll> pOpt = payrollRepository.findById(id);
        if (pOpt.isEmpty()) return ResponseEntity.notFound().build();

        Payroll p = pOpt.get();
        Employee emp = p.getEmployee();
        User u = emp.getUser();
        String[] monthNames = {"","January","February","March","April","May","June","July","August","September","October","November","December"};

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(108, 99, 255));
            Font headFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
            Font netFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(34, 197, 94));

            // Header
            doc.add(new Paragraph("EmPay HRMS", titleFont));
            doc.add(new Paragraph("Payslip — " + monthNames[p.getPayMonth()] + " " + p.getPayYear(), smallFont));
            doc.add(new Paragraph(" "));

            // Employee Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            addInfoCell(infoTable, "Employee", u.getFirstName() + " " + u.getLastName(), headFont, normalFont);
            addInfoCell(infoTable, "Employee Code", emp.getEmployeeCode(), headFont, normalFont);
            addInfoCell(infoTable, "Working Days", p.getPresentDays() + " / " + p.getTotalWorkingDays(), headFont, normalFont);
            addInfoCell(infoTable, "Leaves", String.valueOf(p.getLeavesTaken()), headFont, normalFont);
            doc.add(infoTable);
            doc.add(new Paragraph(" "));

            // Earnings & Deductions side-by-side
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setSpacingBefore(10);

            // Earnings column
            PdfPTable earningsT = new PdfPTable(2);
            earningsT.setWidthPercentage(100);
            addSectionHeader(earningsT, "EARNINGS", headFont);
            addRow(earningsT, "Basic Salary", "₹" + p.getBasicSalary(), normalFont);
            addRow(earningsT, "HRA (40%)", "₹" + p.getHra(), normalFont);
            addRow(earningsT, "Bonus (10%)", "₹" + p.getBonus(), normalFont);
            addRow(earningsT, "Gross Salary", "₹" + p.getGrossSalary(), headFont);

            PdfPCell earningsCell = new PdfPCell(earningsT);
            earningsCell.setBorder(Rectangle.BOX);
            earningsCell.setPadding(8);
            mainTable.addCell(earningsCell);

            // Deductions column
            PdfPTable deductionsT = new PdfPTable(2);
            deductionsT.setWidthPercentage(100);
            addSectionHeader(deductionsT, "DEDUCTIONS", headFont);
            addRow(deductionsT, "Provident Fund (12%)", "- ₹" + p.getPfDeduction(), normalFont);
            addRow(deductionsT, "Professional Tax", "- ₹" + p.getProfessionalTax(), normalFont);
            addRow(deductionsT, "Total Deductions", "- ₹" + p.getTotalDeductions(), headFont);

            PdfPCell deductionsCell = new PdfPCell(deductionsT);
            deductionsCell.setBorder(Rectangle.BOX);
            deductionsCell.setPadding(8);
            mainTable.addCell(deductionsCell);

            doc.add(mainTable);
            doc.add(new Paragraph(" "));

            // Net Pay
            Paragraph netPara = new Paragraph("Net Pay:  ₹" + p.getNetSalary(), netFont);
            netPara.setAlignment(Element.ALIGN_RIGHT);
            doc.add(netPara);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("This is a system-generated payslip and does not require a signature.", smallFont));

            doc.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("payslip_" + emp.getEmployeeCode() + "_" + p.getPayMonth() + "_" + p.getPayYear() + ".pdf").build());
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph(label, new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY)));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private void addSectionHeader(PdfPTable table, String title, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setColspan(2);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setPaddingBottom(6);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPaddingTop(4);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(value, font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPaddingTop(4);
        table.addCell(c2);
    }

    private Payroll calculatePayroll(Employee emp, int month, int year, User generator) {
        BigDecimal basic = emp.getBasicSalary() != null ? emp.getBasicSalary() : new BigDecimal("25000.00");
        int totalDays = 26;
        long presentDays = attendanceRepository.countPresentDays(emp, month, year);
        long approvedLeaves = leaveRepository.countApprovedLeaves(emp, month, year);

        BigDecimal hra = basic.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = basic.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

        boolean noAttendanceData = (presentDays == 0 && approvedLeaves == 0);
        BigDecimal earnedBasic;
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
