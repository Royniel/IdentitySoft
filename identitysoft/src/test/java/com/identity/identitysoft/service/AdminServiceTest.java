package com.identity.identitysoft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.identity.identitysoft.dto.UserSummaryResponse;
import com.identity.identitysoft.entity.Role;
import com.identity.identitysoft.entity.User;
import com.identity.identitysoft.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    AuditService auditService;

    @InjectMocks
    AdminService adminService;

    private User userWithRoles(Long id, String username, Role... roles) {
        return User.builder()
                .id(id)
                .username(username)
                .roles(new HashSet<>(Set.of(roles)))
                .active(true)
                .build();
    }

    @Test
    void makeAdmin_addsAdminRoleAndSaves() {
        User target = userWithRoles(2L, "bob", Role.ROLE_USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummaryResponse response = adminService.makeAdmin(2L);

        assertThat(response.roles()).contains(Role.ROLE_ADMIN, Role.ROLE_USER);
        verify(auditService).log("bob", "MAKE_ADMIN");
    }

    @Test
    void deleteUser_blockedWhenTargetIsTheOnlyRemainingAdmin() {
        User target = userWithRoles(1L, "alice", Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.countByRolesContainingAndIdNot(Role.ROLE_ADMIN, 1L)).thenReturn(0L);

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only remaining admin");

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_succeedsWhenAnotherAdminStillExists() {
        User target = userWithRoles(1L, "alice", Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.countByRolesContainingAndIdNot(Role.ROLE_ADMIN, 1L)).thenReturn(1L);

        adminService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
        verify(auditService).log("alice", "USER_DELETED");
    }

    @Test
    void removeSelfAdmin_blockedWhenCallerIsTheOnlyRemainingAdmin() {
        User self = userWithRoles(1L, "alice", Role.ROLE_ADMIN);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(self));
        when(userRepository.countByRolesContainingAndIdNot(Role.ROLE_ADMIN, 1L)).thenReturn(0L);

        assertThatThrownBy(() -> adminService.removeSelfAdmin("alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only admin");

        verify(userRepository, never()).save(any());
    }

    @Test
    void removeSelfAdmin_succeedsWhenAnotherAdminStillExists() {
        User self = userWithRoles(1L, "alice", Role.ROLE_ADMIN, Role.ROLE_USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(self));
        when(userRepository.countByRolesContainingAndIdNot(Role.ROLE_ADMIN, 1L)).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummaryResponse response = adminService.removeSelfAdmin("alice");

        assertThat(response.roles()).containsExactly(Role.ROLE_USER);
        verify(auditService).log(eq("alice"), eq("SELF_REMOVE_ADMIN"));
    }
}
