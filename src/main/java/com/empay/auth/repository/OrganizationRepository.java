package com.empay.auth.repository;

import com.empay.auth.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByCompanyCode(String companyCode);
}
