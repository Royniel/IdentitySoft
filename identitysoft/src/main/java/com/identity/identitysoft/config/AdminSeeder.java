package com.identity.identitysoft.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;

// Seeds one admin account on startup, but only into a genuinely empty database (e.g. a fresh
// `docker compose up`) — never touches an existing dataset, so it's safe to leave always-on.
@Component
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-admin.username}")
    private String seedUsername;

    @Value("${app.seed-admin.email}")
    private String seedEmail;

    @Value("${app.seed-admin.password}")
    private String seedPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = User.builder()
                .username(seedUsername)
                .email(seedEmail)
                .password(passwordEncoder.encode(seedPassword))
                .active(true)
                .roles(new HashSet<>(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER)))
                .build();

        userRepository.save(admin);
    }
}
