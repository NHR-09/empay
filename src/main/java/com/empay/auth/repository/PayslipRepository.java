package com.empay.auth.repository;

import com.empay.auth.model.PayslipEntity;
import com.empay.auth.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PayslipRepository extends JpaRepository<PayslipEntity, UUID> {
    Optional<PayslipEntity> findByPayroll(Payroll payroll);
}
