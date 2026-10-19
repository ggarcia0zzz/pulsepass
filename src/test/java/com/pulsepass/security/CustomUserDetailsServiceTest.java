package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private UserRoleRepository roleRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private static User user(long id, String username, String email) {
        User user = User.create(username, email);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserCredential credential(User user) {
        return UserCredential.create(user, "$2a$10$hash-ficticio", LocalDateTime.of(2026, 10, 1, 0, 0));
    }

    @Test
    void loadUserByUsername_byUsername_returnsPrincipalWithHashAndRoles() {
        User andrea = user(1L, "andrea", "andrea@email.com");
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential(andrea)));
        when(roleRepository.findByUserId(1L)).thenReturn(List.of(UserRole.of(1L, Role.USER), UserRole.of(1L, Role.STAFF)));

        SecurityPrincipal principal = service.loadUserByUsername("  andrea ");

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hash-ficticio");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.roleNames()).containsExactlyInAnyOrder("USER", "STAFF");
    }

    @Test
    void loadUserByUsername_byEmail_ignoresCase() {
        User andrea = user(1L, "andrea", "andrea@email.com");
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential(andrea)));
        when(roleRepository.findByUserId(1L)).thenReturn(List.of(UserRole.of(1L, Role.USER)));

        assertThat(service.loadUserByUsername("Andrea@Email.com").getId()).isEqualTo(1L);
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_blank_throwsUsernameNotFound() {
        assertThatThrownBy(() -> service.loadUserByUsername("  ")).isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Usuario legado sin credencial: no puede iniciar sesión (sin contraseña por defecto)")
    void loadUserByUsername_legacyUserWithoutCredential_cannotLogin() {
        User legacy = user(5L, "legado", "legado@email.com");
        when(userRepository.findByUsername("legado")).thenReturn(Optional.of(legacy));
        when(credentialRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("legado")).isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void loadUserByUsername_inactiveUser_isNotEnabled() {
        User inactive = user(2L, "carlos", "carlos@email.com");
        inactive.deactivate();
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(inactive));
        when(credentialRepository.findById(2L)).thenReturn(Optional.of(credential(inactive)));
        when(roleRepository.findByUserId(2L)).thenReturn(List.of(UserRole.of(2L, Role.USER)));

        assertThat(service.loadUserByUsername("carlos").isEnabled()).isFalse();
    }

    @Test
    void loadUserById_reloadsCurrentRolesAndState() {
        User laura = user(3L, "laura", "laura@email.com");
        when(userRepository.findById(3L)).thenReturn(Optional.of(laura));
        when(roleRepository.findByUserId(3L)).thenReturn(List.of(UserRole.of(3L, Role.STAFF)));

        SecurityPrincipal principal = service.loadUserById(3L);

        assertThat(principal.roleNames()).containsExactly("STAFF");
        assertThat(principal.hasRole(Role.ADMIN)).isFalse();
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void loadUserById_unknown_throwsUsernameNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(99L)).isInstanceOf(UsernameNotFoundException.class);
    }
}
