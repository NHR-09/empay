package com.empay.auth.repository;

import com.empay.auth.model.Employee;
import com.empay.auth.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRepository extends JpaRepository<Payroll, UUID> {
    Optional<Payroll> findByEmployeeAndPayMonthAndPayYear(Employee employee, int month, int year);
    List<Payroll> findByEmployeeOrderByPayYearDescPayMonthDesc(Employee employee);
    List<Payroll> findByOrganizationAndPayMonthAndPayYear(com.empay.auth.model.Organization organization, int payMonth, int payYear);

    @Query("SELECT p FROM Payroll p WHERE p.organization.id = :orgId AND p.payMonth = :month AND p.payYear = :year")
    List<Payroll> findByOrgAndMonthYear(@Param("orgId") UUID orgId, @Param("month") int month, @Param("year") int year);
}
