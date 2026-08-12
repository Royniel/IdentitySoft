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
import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;
import com.identity.identitysoft.service.AuditService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminController(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
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
        User user = getUserOrThrow(id);
        user.getRoles().add(Role.ROLE_ADMIN);
        auditService.log(user.getUsername(), "MAKE_ADMIN");
        return UserSummaryResponse.from(userRepository.save(user));
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        User user = getUserOrThrow(id);

        if (user.getRoles().contains(Role.ROLE_ADMIN) && countOtherAdmins(id) == 0) {
            throw new IllegalArgumentException(
                    "Cannot delete the only remaining admin. Promote another user first.");
        }

        userRepository.deleteById(id);
        auditService.log(user.getUsername(), "USER_DELETED");
    }

    // Lets an admin voluntarily give up their own admin role, but only if someone else can still manage the system.
    @PutMapping("/self/remove-admin")
    public UserSummaryResponse removeSelfAdmin(Authentication authentication) {
        User self = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (countOtherAdmins(self.getId()) == 0) {
            throw new IllegalArgumentException(
                    "You are the only admin. Promote another user before removing yourself.");
        }

        self.getRoles().remove(Role.ROLE_ADMIN);
        auditService.log(self.getUsername(), "SELF_REMOVE_ADMIN");
        return UserSummaryResponse.from(userRepository.save(self));
    }

    @GetMapping("/audit/{username}")
    public List<AuditLog> getAuditLogs(@PathVariable String username) {
        return auditService.getLogsForUser(username);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private long countOtherAdmins(Long excludingId) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(excludingId) && u.getRoles().contains(Role.ROLE_ADMIN))
                .count();
    }
}