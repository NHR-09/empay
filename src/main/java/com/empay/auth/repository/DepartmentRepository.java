package com.empay.auth.repository;

import com.empay.auth.model.Department;
import com.empay.auth.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByOrganization(Organization organization);
}
