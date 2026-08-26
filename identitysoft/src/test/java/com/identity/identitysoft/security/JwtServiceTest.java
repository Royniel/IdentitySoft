package com.identity.identitysoft.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        // @Value fields aren't populated outside a Spring context, so wire them by hand.
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-for-unit-tests-0123456789");
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 900_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604_800_000L);
    }

    @Test
    void generatedToken_roundTripsToTheSameUsername_andIsValid() {
        String token = jwtService.generateAccessToken("alice");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, "alice")).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_whenSubjectDoesNotMatchExpectedUsername() {
        String token = jwtService.generateAccessToken("alice");

        assertThat(jwtService.isTokenValid(token, "bob")).isFalse();
    }

    @Test
    void extractUsername_throwsExpiredJwtException_onAnAlreadyExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessExpiration", -10_000L); // expired the instant it's issued
        String token = jwtService.generateAccessToken("alice");

        // jjwt validates registered claims (exp) during parsing, so this throws rather than
        // returning a value — callers (JwtAuthenticationFilter) must catch JwtException.
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
