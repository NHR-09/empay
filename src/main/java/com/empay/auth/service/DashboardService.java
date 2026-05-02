package com.empay.auth.service;

import com.empay.auth.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {
    private final CacheService cache;
    private final AttendanceRepository attendanceRepo;
    private final LeaveRequestRepository leaveRepo;
    private final PayrollRepository payrollRepo;
    private final EmployeeRepository employeeRepo;
    
    public DashboardService(CacheService cache, AttendanceRepository attendanceRepo,
                          LeaveRequestRepository leaveRepo, PayrollRepository payrollRepo,
                          EmployeeRepository employeeRepo) {
        this.cache = cache;
        this.attendanceRepo = attendanceRepo;
        this.leaveRepo = leaveRepo;
        this.payrollRepo = payrollRepo;
        this.employeeRepo = employeeRepo;
    }
    
    public Map<String, Object> getSummary(UUID orgId, String userEmail) {
        String cacheKey = "dashboard:" + orgId + ":" + userEmail;
        Object cached = cache.get(cacheKey);
        if (cached != null) return (Map<String, Object>) cached;
        
        Map<String, Object> summary = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        // Counters
        summary.put("totalEmployees", employeeRepo.countByOrganizationId(orgId));
        summary.put("presentToday", attendanceRepo.countByOrganizationIdAndAttendanceDate(orgId, today));
        summary.put("pendingLeaves", leaveRepo.countByOrganizationIdAndStatus(orgId, "PENDING"));
        summary.put("payrollThisMonth", payrollRepo.countByOrganizationIdAndPayMonthAndPayYear(
            orgId, today.getMonthValue(), today.getYear()));
        
        cache.put(cacheKey, summary, 300); // 5 min TTL
        return summary;
    }
}
