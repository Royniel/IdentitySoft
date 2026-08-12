package com.identity.identitysoft.dto;

import java.util.Set;

import com.identity.identitysoft.entity.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String username,
        Set<Role> roles
) {}