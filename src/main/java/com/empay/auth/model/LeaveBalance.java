package com.empay.auth.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "leave_balance", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type", "year"}))
public class LeaveBalance {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "leave_type", nullable = false, length = 50)
    private String leaveType; // CASUAL, SICK, EARNED

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "used_days", nullable = false)
    private int usedDays = 0;

    @Column(name = "remaining_days", nullable = false)
    private int remainingDays;

    @Column(nullable = false)
    private int year;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public int getUsedDays() { return usedDays; }
    public void setUsedDays(int usedDays) { this.usedDays = usedDays; }
    public int getRemainingDays() { return remainingDays; }
    public void setRemainingDays(int remainingDays) { this.remainingDays = remainingDays; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
