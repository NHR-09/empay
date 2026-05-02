package com.empay.auth.service;

import com.empay.auth.model.LeaveRequest;
import com.empay.auth.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveService {
    private final CacheService cache;
    private final LeaveRequestRepository leaveRepo;
    
    public LeaveService(CacheService cache, LeaveRequestRepository leaveRepo) {
        this.cache = cache;
        this.leaveRepo = leaveRepo;
    }
    
    public Queue<Map<String, Object>> getPendingQueue(UUID orgId) {
        String cacheKey = "leave_queue:" + orgId;
        Object cached = cache.get(cacheKey);
        if (cached != null) return (Queue<Map<String, Object>>) cached;
        
        Queue<Map<String, Object>> queue = leaveRepo.findByOrganizationIdAndStatusOrderByRequestedAtAsc(orgId, "PENDING")
            .stream()
            .map(this::toMap)
            .collect(Collectors.toCollection(LinkedList::new));
        
        cache.put(cacheKey, queue, 300); // 5 min
        return queue;
    }
    
    public Map<String, Long> getLeaveBalance(UUID empId) {
        String cacheKey = "leave_balance:" + empId;
        Object cached = cache.get(cacheKey);
        if (cached != null) return (Map<String, Long>) cached;
        
        List<LeaveRequest> approved = leaveRepo.findByEmployeeIdAndStatus(empId, "APPROVED");
        
        Map<String, Long> balance = approved.stream()
            .collect(Collectors.groupingBy(LeaveRequest::getLeaveType, 
                Collectors.summingLong(lr -> lr.getEndDate().toEpochDay() - lr.getStartDate().toEpochDay() + 1)));
        
        cache.put(cacheKey, balance, 3600); // 1 hour
        return balance;
    }
    
    private Map<String, Object> toMap(LeaveRequest lr) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", lr.getId());
        m.put("employeeName", lr.getEmployee().getUser().getFirstName() + " " + lr.getEmployee().getUser().getLastName());
        m.put("leaveType", lr.getLeaveType());
        m.put("startDate", lr.getStartDate());
        m.put("endDate", lr.getEndDate());
        m.put("reason", lr.getReason());
        return m;
    }
    
    public void invalidateCache(UUID orgId, UUID empId) {
        cache.invalidate("leave_queue:" + orgId);
        cache.invalidate("leave_balance:" + empId);
    }
}
