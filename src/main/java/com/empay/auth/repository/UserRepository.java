package com.empay.auth.repository;

import com.empay.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :orgId AND YEAR(u.createdAt) = :year")
    long countByOrgAndYear(@Param("orgId") UUID orgId, @Param("year") int year);
}
