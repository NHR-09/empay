package com.empay.auth.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    public void put(String key, Object value, long ttlSeconds) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000));
    }
    
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }
    
    public void invalidate(String key) {
        cache.remove(key);
    }
    
    public void invalidatePattern(String pattern) {
        cache.keySet().removeIf(k -> k.startsWith(pattern));
    }
    
    private static class CacheEntry {
        final Object value;
        final long expiryTime;
        
        CacheEntry(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
