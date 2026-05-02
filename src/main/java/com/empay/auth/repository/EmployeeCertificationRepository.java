package com.empay.auth.repository;

import com.empay.auth.model.EmployeeCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EmployeeCertificationRepository extends JpaRepository<EmployeeCertification, UUID> {
    List<EmployeeCertification> findByEmployeeId(UUID employeeId);
}
