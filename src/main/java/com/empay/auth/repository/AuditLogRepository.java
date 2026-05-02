package com.empay.auth.repository;

import com.empay.auth.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);
}
