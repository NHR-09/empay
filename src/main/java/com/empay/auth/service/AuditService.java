package com.empay.auth.service;

import com.empay.auth.model.AuditLog;
import com.empay.auth.model.Notification;
import com.empay.auth.model.User;
import com.empay.auth.repository.AuditLogRepository;
import com.empay.auth.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    public AuditService(AuditLogRepository auditLogRepository, NotificationRepository notificationRepository) {
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    public void log(User user, String action, String module, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setModule(module);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        auditLogRepository.save(log);
    }

    public void notify(User user, String message, String type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        n.setType(type);
        notificationRepository.save(n);
    }
}
