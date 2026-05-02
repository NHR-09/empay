package com.empay.auth.repository;

import com.empay.auth.model.Deduction;
import com.empay.auth.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeductionRepository extends JpaRepository<Deduction, UUID> {
    List<Deduction> findByPayroll(Payroll payroll);
}
