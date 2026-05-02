package com.empay.auth.repository;

import com.empay.auth.model.Employee;
import com.empay.auth.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(Employee employee);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.organization.id = :orgId ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByOrgId(@Param("orgId") UUID orgId);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.employee = :emp AND l.status = 'APPROVED' AND MONTH(l.startDate) = :month AND YEAR(l.startDate) = :year")
    long countApprovedLeaves(@Param("emp") Employee emp, @Param("month") int month, @Param("year") int year);
    
    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.employee.organization.id = :orgId AND l.status = :status")
    long countByOrganizationIdAndStatus(@Param("orgId") UUID orgId, @Param("status") String status);
    
    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.organization.id = :orgId AND l.status = :status ORDER BY l.createdAt ASC")
    List<LeaveRequest> findByOrganizationIdAndStatusOrderByRequestedAtAsc(@Param("orgId") UUID orgId, @Param("status") String status);
    
    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.id = :empId AND l.status = :status")
    List<LeaveRequest> findByEmployeeIdAndStatus(@Param("empId") UUID empId, @Param("status") String status);
}
