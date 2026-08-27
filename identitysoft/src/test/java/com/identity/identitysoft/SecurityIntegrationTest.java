package com.identity.identitysoft;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Hits real endpoints through the full Spring Security filter chain (JwtAuthenticationFilter,
 * method security, etc.) against a real Postgres instance, rather than a mocked repository layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String registerAndLogin(String username, String email) throws Exception {
        String password = "TestPass1!";
        String registerBody = objectMapper.writeValueAsString(
                Map.of("username", username, "email", email, "password", password));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk());
        return loginAndExtractToken(username, password);
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    @Test
    void adminEndpoint_withAdminToken_returnsTheUserList() throws Exception {
        String rawPassword = "TestPass1!";
        User admin = User.builder()
                .username("it-admin")
                .email("it-admin@test.com")
                .password(passwordEncoder.encode(rawPassword))
                .active(true)
                .roles(new HashSet<>(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER)))
                .build();
        userRepository.save(admin);
        String token = loginAndExtractToken("it-admin", rawPassword);

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    void adminEndpoint_withNonAdminToken_isForbidden_insufficientRole() throws Exception {
        String token = registerAndLogin("it-plainuser", "it-plainuser@test.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withNoTokenAtAll_isForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withAMalformedToken_isRejectedCleanly_not500() throws Exception {
        // Regression test: JwtAuthenticationFilter must catch JWT parsing errors and treat the
        // request as unauthenticated, instead of letting an unhandled exception escape.
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isForbidden());
    }
}
