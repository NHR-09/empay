package com.empay.auth.service;

import com.empay.auth.model.Employee;
import com.empay.auth.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    private final CacheService cache;
    private final EmployeeRepository employeeRepo;
    
    public EmployeeService(CacheService cache, EmployeeRepository employeeRepo) {
        this.cache = cache;
        this.employeeRepo = employeeRepo;
    }
    
    public List<Map<String, Object>> getEmployeeList(UUID orgId) {
        String cacheKey = "employees:" + orgId;
        Object cached = cache.get(cacheKey);
        if (cached != null) return (List<Map<String, Object>>) cached;
        
        List<Map<String, Object>> result = employeeRepo.findByOrganizationId(orgId)
            .stream()
            .map(this::toMap)
            .collect(Collectors.toList());
        
        cache.put(cacheKey, result, 600); // 10 min
        return result;
    }
    
    public Map<String, Employee> getEmployeeMapByCode(UUID orgId) {
        return employeeRepo.findByOrganizationId(orgId)
            .stream()
            .collect(Collectors.toMap(Employee::getEmployeeCode, e -> e));
    }
    
    private Map<String, Object> toMap(Employee emp) {
        Map<String, Object> m = new HashMap<>();
        m.put("employeeCode", emp.getEmployeeCode());
        m.put("firstName", emp.getUser().getFirstName());
        m.put("lastName", emp.getUser().getLastName());
        m.put("email", emp.getUser().getEmail());
        m.put("designation", emp.getDesignation());
        m.put("status", emp.getStatus());
        return m;
    }
    
    public void invalidateCache(UUID orgId) {
        cache.invalidate("employees:" + orgId);
    }
}
