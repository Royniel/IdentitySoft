package com.identity.identitysoft.service;

import org.springframework.stereotype.Service;

import com.identity.identitysoft.dto.UserSummaryResponse;
import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public UserSummaryResponse makeAdmin(Long id) {
        User user = getUserOrThrow(id);
        user.getRoles().add(Role.ROLE_ADMIN);
        auditService.log(user.getUsername(), "MAKE_ADMIN");
        return UserSummaryResponse.from(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = getUserOrThrow(id);

        if (user.getRoles().contains(Role.ROLE_ADMIN) && countOtherAdmins(id) == 0) {
            throw new IllegalArgumentException(
                    "Cannot delete the only remaining admin. Promote another user first.");
        }

        userRepository.deleteById(id);
        auditService.log(user.getUsername(), "USER_DELETED");
    }

    // Lets an admin voluntarily give up their own admin role, but only if someone else can still manage the system.
    public UserSummaryResponse removeSelfAdmin(String username) {
        User self = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (countOtherAdmins(self.getId()) == 0) {
            throw new IllegalArgumentException(
                    "You are the only admin. Promote another user before removing yourself.");
        }

        self.getRoles().remove(Role.ROLE_ADMIN);
        auditService.log(self.getUsername(), "SELF_REMOVE_ADMIN");
        return UserSummaryResponse.from(userRepository.save(self));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private long countOtherAdmins(Long excludingId) {
        return userRepository.countByRolesContainingAndIdNot(Role.ROLE_ADMIN, excludingId);
    }
}
