package com.empay.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "deductions")
public class Deduction {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "payroll_id", nullable = false)
    private Payroll payroll;

    @Column(name = "deduction_type", nullable = false, length = 100)
    private String deductionType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Payroll getPayroll() { return payroll; }
    public void setPayroll(Payroll payroll) { this.payroll = payroll; }
    public String getDeductionType() { return deductionType; }
    public void setDeductionType(String deductionType) { this.deductionType = deductionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
