package com.empay.auth;

import com.empay.auth.model.Organization;
import com.empay.auth.model.RoleEntity;
import com.empay.auth.model.User;
import com.empay.auth.repository.OrganizationRepository;
import com.empay.auth.repository.RoleRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository,
                                OrganizationRepository organizationRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed default organization if not exists
            Organization org = organizationRepository.findByCompanyCode("OI")
                    .orElseGet(() -> {
                        Organization o = new Organization();
                        o.setCompanyName("Odoo India");
                        o.setCompanyCode("OI");
                        o.setEmail("admin@odooindia.com");
                        o.setSubscriptionPlan("ENTERPRISE");
                        return organizationRepository.save(o);
                    });

            // Seed admin user if not exists
            if (userRepository.findByEmail("admin@odooindia.com").isEmpty()) {
                RoleEntity adminRole = roleRepository.findByRoleName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("ADMIN role not found in DB. Run empay_schema.sql first."));

                User admin = new User();
                admin.setFirstName("Super");
                admin.setLastName("Admin");
                admin.setEmail("admin@odooindia.com");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setOrganization(org);
                admin.setRole(adminRole);
                admin.setMustChangePassword(false);
                userRepository.save(admin);

                System.out.println("✅ Admin user seeded: admin@odooindia.com / Admin@123");
            }
        };
    }
}
