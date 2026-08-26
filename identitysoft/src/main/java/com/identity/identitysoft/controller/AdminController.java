package com.identity.identitysoft.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.identity.identitysoft.dto.UserSummaryResponse;
import com.identity.identitysoft.entity.AuditLog;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;
import com.identity.identitysoft.service.AdminService;
import com.identity.identitysoft.service.AuditService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AdminService adminService;

    public AdminController(UserRepository userRepository, AuditService auditService, AdminService adminService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @PutMapping("/users/{id}/deactivate")
    public UserSummaryResponse deactivateUser(@PathVariable Long id) {
        User user = getUserOrThrow(id);
        user.setActive(false);
        auditService.log(user.getUsername(), "DEACTIVATE");
        return UserSummaryResponse.from(userRepository.save(user));
    }

    @PutMapping("/users/{id}/activate")
    public UserSummaryResponse activateUser(@PathVariable Long id) {
        User user = getUserOrThrow(id);
        user.setActive(true);
        auditService.log(user.getUsername(), "ACTIVATE");
        return UserSummaryResponse.from(userRepository.save(user));
    }

    @PutMapping("/users/{id}/make-admin")
    public UserSummaryResponse makeAdmin(@PathVariable Long id) {
        return adminService.makeAdmin(id);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
    }

    @PutMapping("/self/remove-admin")
    public UserSummaryResponse removeSelfAdmin(Authentication authentication) {
        return adminService.removeSelfAdmin(authentication.getName());
    }

    @GetMapping("/audit/{username}")
    public List<AuditLog> getAuditLogs(@PathVariable String username) {
        return auditService.getLogsForUser(username);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
