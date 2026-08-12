package com.identity.identitysoft.service;

import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.identity.identitysoft.dto.AuthResponse;
import com.identity.identitysoft.dto.ForgotPasswordRequest;
import com.identity.identitysoft.dto.LoginRequest;
import com.identity.identitysoft.dto.RefreshRequest;
import com.identity.identitysoft.dto.RegisterRequest;
import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;
import com.identity.identitysoft.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    // ===== REGISTER =====
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        userRepository.save(user);
        auditService.log(user.getUsername(), "REGISTER");

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getRoles());
    }

    // ===== LOGIN =====
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));
        } catch (Exception e) {
            auditService.log(request.username(), "LOGIN_FAILURE");
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        auditService.log(request.username(), "LOGIN_SUCCESS");
        String accessToken = jwtService.generateAccessToken(request.username());
        String refreshToken = jwtService.generateRefreshToken(request.username());
        return new AuthResponse(accessToken, refreshToken, request.username(), user.getRoles());
    }

    // ===== REFRESH =====
    public AuthResponse refresh(RefreshRequest request) {
        String username = jwtService.extractUsername(request.refreshToken());

        if (username == null || !jwtService.isTokenValid(request.refreshToken(), username)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        String newAccessToken = jwtService.generateAccessToken(username);
        return new AuthResponse(newAccessToken, request.refreshToken(), username, user.getRoles());
    }

    // ===== FORGOT PASSWORD (demo: identity is confirmed by username/email only, no email verification) =====
    public void resetPassword(ForgotPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        User user = userRepository.findByUsername(request.identifier())
                .or(() -> userRepository.findByEmail(request.identifier()))
                .orElseThrow(() -> new IllegalArgumentException("No account found with that username or email"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.log(user.getUsername(), "PASSWORD_RESET");
    }
}