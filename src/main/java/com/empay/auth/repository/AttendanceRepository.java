package com.empay.auth.repository;

import com.empay.auth.model.Attendance;
import com.empay.auth.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByEmployeeAndAttendanceDate(Employee employee, LocalDate date);
    List<Attendance> findByEmployeeOrderByAttendanceDateDesc(Employee employee);

    @Query("SELECT a FROM Attendance a WHERE a.employee.organization.id = :orgId AND a.attendanceDate = :date")
    List<Attendance> findByOrgAndDate(@Param("orgId") UUID orgId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.employee.organization.id = :orgId AND MONTH(a.attendanceDate) = :month AND YEAR(a.attendanceDate) = :year")
    List<Attendance> findByOrgAndMonthYear(@Param("orgId") UUID orgId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee = :emp AND MONTH(a.attendanceDate) = :month AND YEAR(a.attendanceDate) = :year AND a.status = 'PRESENT'")
    long countPresentDays(@Param("emp") Employee emp, @Param("month") int month, @Param("year") int year);
}
