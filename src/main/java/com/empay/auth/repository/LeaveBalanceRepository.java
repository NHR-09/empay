package com.empay.auth.repository;

import com.empay.auth.model.Employee;
import com.empay.auth.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    List<LeaveBalance> findByEmployeeAndYear(Employee employee, int year);
    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndYear(Employee employee, String leaveType, int year);
}
