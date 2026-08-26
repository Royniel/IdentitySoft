package com.identity.identitysoft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.identity.identitysoft.dto.AuthResponse;
import com.identity.identitysoft.dto.LoginRequest;
import com.identity.identitysoft.dto.RefreshRequest;
import com.identity.identitysoft.dto.RegisterRequest;
import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;
import com.identity.identitysoft.security.JwtService;

import io.jsonwebtoken.ExpiredJwtException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    AuditService auditService;

    @InjectMocks
    AuthService authService;

    // ---------- register ----------

    @Test
    void register_success_savesEncodedPasswordWithDefaultRole_andReturnsTokens() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "Passw0rd!");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed");
        when(jwtService.generateAccessToken("alice")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("alice")).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.roles()).containsExactly(Role.ROLE_USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        verify(auditService).log("alice", "REGISTER");
    }

    @Test
    void register_duplicateUsername_throwsAndNeverSaves() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "Passw0rd!");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsAndNeverSaves() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "Passw0rd!");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_success_returnsTokensAndLogsSuccess() {
        LoginRequest request = new LoginRequest("alice", "Passw0rd!");
        User user = User.builder().username("alice").roles(Set.of(Role.ROLE_USER)).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("alice")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("alice")).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(auditService).log("alice", "LOGIN_SUCCESS");
    }

    @Test
    void login_badCredentials_throwsGenericMessage_andLogsFailure() {
        LoginRequest request = new LoginRequest("alice", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");

        verify(auditService).log("alice", "LOGIN_FAILURE");
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void login_disabledAccount_throwsTheSameGenericMessage_soAccountStatusIsNotLeaked() {
        LoginRequest request = new LoginRequest("alice", "Passw0rd!");
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");

        verify(auditService).log("alice", "LOGIN_FAILURE");
    }

    // ---------- refresh ----------

    @Test
    void refresh_validToken_returnsNewAccessTokenAndKeepsRefreshToken() {
        RefreshRequest request = new RefreshRequest("refresh-token");
        User user = User.builder().username("alice").roles(Set.of(Role.ROLE_USER)).build();
        when(jwtService.extractUsername("refresh-token")).thenReturn("alice");
        when(jwtService.isTokenValid("refresh-token", "alice")).thenReturn(true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("alice")).thenReturn("new-access-token");

        AuthResponse response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refresh_tokenThatFailsValidation_throwsIllegalArgumentException() {
        RefreshRequest request = new RefreshRequest("refresh-token");
        when(jwtService.extractUsername("refresh-token")).thenReturn("alice");
        when(jwtService.isTokenValid("refresh-token", "alice")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    // NOTE: this documents CURRENT behavior — see the message to the user before this file
    // was finalized. refresh() does not catch the exception JwtService throws for an actually
    // expired/malformed token, so it does NOT get converted to the intended, friendly
    // IllegalArgumentException("Invalid or expired refresh token"). It escapes as-is.
    @Test
    void refresh_whenTokenIsActuallyExpired_escapesAsJwtException_notTheIntendedFriendlyMessage() {
        RefreshRequest request = new RefreshRequest("some-expired-token");
        when(jwtService.extractUsername("some-expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
