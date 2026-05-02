package com.empay.auth.repository;

import com.empay.auth.model.Employee;
import com.empay.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByUser(User user);
    List<Employee> findByOrganizationId(UUID orgId);
}
