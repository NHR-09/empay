package com.empay.auth.service;

import com.empay.auth.model.Organization;
import com.empay.auth.model.RoleEntity;
import com.empay.auth.model.User;
import com.empay.auth.repository.OrganizationRepository;
import com.empay.auth.repository.RoleRepository;
import com.empay.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";

    public UserService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public User registerUser(String firstName, String lastName, String email,
                             String phone, String companyCode, String roleName) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("A user with this email already exists.");
        }

        Organization organization = organizationRepository.findByCompanyCode(companyCode)
                .orElseThrow(() -> new RuntimeException("Organization not found for code: " + companyCode));

        RoleEntity role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        int year = LocalDateTime.now().getYear();
        long serial = userRepository.countByOrgAndYear(organization.getId(), year) + 1;
        String loginId = generateLoginId(firstName, lastName, year, serial);

        String tempPassword = generateTempPassword();

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setOrganization(organization);
        user.setRole(role);
        user.setLoginId(loginId);
        user.setMustChangePassword(true);

        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("A user with this email already exists.");
        }
        emailService.sendTempPassword(email, firstName, tempPassword, loginId);
        return user;
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    private String generateLoginId(String firstName, String lastName, int year, long serial) {
        String first2 = firstName.length() >= 2 ? firstName.substring(0, 2).toUpperCase() : firstName.toUpperCase();
        String last2  = lastName.length()  >= 2 ? lastName.substring(0, 2).toUpperCase()  : lastName.toUpperCase();
        String serialStr = String.format("%04d", serial);
        return "OI" + first2 + last2 + year + serialStr;
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
