package com.empay.auth.repository;

import com.empay.auth.model.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    List<EmployeeDocument> findByEmployeeId(UUID employeeId);
    List<EmployeeDocument> findByEmployeeIdAndDocumentType(UUID employeeId, String documentType);
}
