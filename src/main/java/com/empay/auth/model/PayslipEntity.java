package com.empay.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payslips")
public class PayslipEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne @JoinColumn(name = "payroll_id", nullable = false, unique = true)
    private Payroll payroll;

    @Column(name = "payslip_url", columnDefinition = "TEXT")
    private String payslipUrl;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Payroll getPayroll() { return payroll; }
    public void setPayroll(Payroll payroll) { this.payroll = payroll; }
    public String getPayslipUrl() { return payslipUrl; }
    public void setPayslipUrl(String payslipUrl) { this.payslipUrl = payslipUrl; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
}
