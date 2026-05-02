package com.empay.auth.controller;

import com.empay.auth.model.*;
import com.empay.auth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private EmployeeSkillRepository skillRepository;
    @Autowired private EmployeeCertificationRepository certificationRepository;
    @Autowired private EmployeeDocumentRepository documentRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/skills")
    public ResponseEntity<?> getSkills(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Employee emp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
        
        return ResponseEntity.ok(skillRepository.findByEmployeeId(emp.getId()));
    }

    @PostMapping("/skills")
    public ResponseEntity<?> addSkill(@RequestBody Map<String, Object> req) {
        String email = (String) req.get("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Employee emp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
        
        EmployeeSkill skill = new EmployeeSkill();
        skill.setEmployee(emp);
        skill.setSkillName((String) req.get("skillName"));
        skill.setProficiencyLevel((String) req.get("proficiencyLevel"));
        skill.setYearsOfExperience((Integer) req.get("yearsOfExperience"));
        
        skillRepository.save(skill);
        return ResponseEntity.ok(Map.of("message", "Skill added successfully"));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable UUID id) {
        skillRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Skill deleted"));
    }

    @GetMapping("/certifications")
    public ResponseEntity<?> getCertifications(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Employee emp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
        
        return ResponseEntity.ok(certificationRepository.findByEmployeeId(emp.getId()));
    }

    @PostMapping("/certifications")
    public ResponseEntity<?> addCertification(@RequestBody Map<String, Object> req) {
        String email = (String) req.get("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Employee emp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
        
        EmployeeCertification cert = new EmployeeCertification();
        cert.setEmployee(emp);
        cert.setCertificationName((String) req.get("certificationName"));
        cert.setIssuingOrganization((String) req.get("issuingOrganization"));
        cert.setCredentialId((String) req.get("credentialId"));
        cert.setCredentialUrl((String) req.get("credentialUrl"));
        
        certificationRepository.save(cert);
        return ResponseEntity.ok(Map.of("message", "Certification added successfully"));
    }

    @DeleteMapping("/certifications/{id}")
    public ResponseEntity<?> deleteCertification(@PathVariable UUID id) {
        certificationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Certification deleted"));
    }

    @GetMapping("/documents")
    public ResponseEntity<?> getDocuments(@RequestParam String email, @RequestParam(required = false) String type) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Employee emp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
        
        if (type != null) {
            return ResponseEntity.ok(documentRepository.findByEmployeeIdAndDocumentType(emp.getId(), type));
        }
        return ResponseEntity.ok(documentRepository.findByEmployeeId(emp.getId()));
    }

    @PostMapping("/documents")
    public ResponseEntity<?> addDocument(@RequestBody Map<String, Object> req) {
        String email = (String) req.get("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        
        Employee emp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
        
        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(emp);
        doc.setDocumentType((String) req.get("documentType"));
        doc.setDocumentName((String) req.get("documentName"));
        doc.setDocumentUrl((String) req.get("documentUrl"));
        
        documentRepository.save(doc);
        return ResponseEntity.ok(Map.of("message", "Document added successfully"));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID id) {
        documentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }
}
