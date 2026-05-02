package com.empay.auth.repository;

import com.empay.auth.model.Employee;
import com.empay.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByUser(User user);
    Optional<Employee> findByUserId(UUID userId);
    List<Employee> findByOrganizationId(UUID orgId);
    Optional<Employee> findByEmployeeCode(String employeeCode);
    List<Employee> findByHrManager(User hrManager);
    
    long countByOrganizationId(UUID orgId);
    
    @Query("SELECT e.employeeCode, e.user.firstName, e.user.lastName FROM Employee e WHERE e.organization.id = :orgId")
    List<Object[]> findBasicInfoByOrganizationId(@Param("orgId") UUID orgId);
}
