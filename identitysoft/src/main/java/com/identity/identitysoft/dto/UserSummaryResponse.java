package com.identity.identitysoft.dto;

import java.util.Set;

import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;

public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        boolean active,
        Set<Role> roles
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                user.getRoles()
        );
    }
}
