package com.empay.auth.service;

import com.empay.auth.model.Attendance;
import com.empay.auth.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    private final CacheService cache;
    private final AttendanceRepository attendanceRepo;
    
    public AttendanceService(CacheService cache, AttendanceRepository attendanceRepo) {
        this.cache = cache;
        this.attendanceRepo = attendanceRepo;
    }
    
    public Map<String, Object> getMonthlyStats(UUID empId, int month, int year) {
        String cacheKey = "att_stats:" + empId + ":" + month + ":" + year;
        Object cached = cache.get(cacheKey);
        if (cached != null) return (Map<String, Object>) cached;
        
        List<Attendance> records = attendanceRepo.findByEmployeeIdAndMonthYear(empId, month, year);
        
        Map<String, Long> statusCount = records.stream()
            .collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting()));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", records.size());
        stats.put("present", statusCount.getOrDefault("PRESENT", 0L));
        stats.put("absent", statusCount.getOrDefault("ABSENT", 0L));
        stats.put("halfDay", statusCount.getOrDefault("HALF_DAY", 0L));
        
        cache.put(cacheKey, stats, 1800); // 30 min
        return stats;
    }
    
    public void invalidateCache(UUID empId) {
        cache.invalidatePattern("att_stats:" + empId);
    }
}
