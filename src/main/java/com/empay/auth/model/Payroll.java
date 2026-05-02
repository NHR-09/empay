package com.empay.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "pay_month", "pay_year"}))
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "pay_month", nullable = false)
    private int payMonth;

    @Column(name = "pay_year", nullable = false)
    private int payYear;

    @Column(name = "total_working_days")
    private int totalWorkingDays = 26;

    @Column(name = "present_days")
    private int presentDays = 0;

    @Column(name = "leaves_taken")
    private int leavesTaken = 0;

    @Column(name = "basic_salary", precision = 12, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "gross_salary", precision = 12, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(name = "pf_deduction", precision = 12, scale = 2)
    private BigDecimal pfDeduction = BigDecimal.ZERO;

    @Column(name = "professional_tax", precision = 12, scale = 2)
    private BigDecimal professionalTax = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 12, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_salary", precision = 12, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "payroll_status")
    private String payrollStatus = "GENERATED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public int getPayMonth() { return payMonth; }
    public void setPayMonth(int payMonth) { this.payMonth = payMonth; }
    public int getPayYear() { return payYear; }
    public void setPayYear(int payYear) { this.payYear = payYear; }
    public int getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(int totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }
    public int getPresentDays() { return presentDays; }
    public void setPresentDays(int presentDays) { this.presentDays = presentDays; }
    public int getLeavesTaken() { return leavesTaken; }
    public void setLeavesTaken(int leavesTaken) { this.leavesTaken = leavesTaken; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal hra) { this.hra = hra; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    public BigDecimal getPfDeduction() { return pfDeduction; }
    public void setPfDeduction(BigDecimal pfDeduction) { this.pfDeduction = pfDeduction; }
    public BigDecimal getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public String getPayrollStatus() { return payrollStatus; }
    public void setPayrollStatus(String payrollStatus) { this.payrollStatus = payrollStatus; }
    public User getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(User generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
