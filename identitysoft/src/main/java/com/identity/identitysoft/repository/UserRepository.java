package com.identity.identitysoft.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRolesContainingAndIdNot(Role role, Long id);
}