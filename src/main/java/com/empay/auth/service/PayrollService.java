package com.empay.auth.service;

import com.empay.auth.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PayrollService {
    private final CacheService cache;
    private final PayrollRepository payrollRepo;
    
    public PayrollService(CacheService cache, PayrollRepository payrollRepo) {
        this.cache = cache;
        this.payrollRepo = payrollRepo;
    }
    
    public Map<String, Object> getMonthlyReport(UUID orgId, int month, int year) {
        String cacheKey = "payroll_report:" + orgId + ":" + month + ":" + year;
        Object cached = cache.get(cacheKey);
        if (cached != null) return (Map<String, Object>) cached;
        
        List<Object[]> data = payrollRepo.findMonthlySummary(orgId, month, year);
        
        Map<String, Object> report = new HashMap<>();
        if (!data.isEmpty()) {
            Object[] row = data.get(0);
            report.put("totalEmployees", row[0]);
            report.put("totalGross", row[1]);
            report.put("totalDeductions", row[2]);
            report.put("totalNet", row[3]);
        }
        
        cache.put(cacheKey, report, 1800); // 30 min
        return report;
    }
    
    public void invalidateCache(UUID orgId) {
        cache.invalidatePattern("payroll_report:" + orgId);
    }
}
