package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCredentialRepository credentialRepository;
    @Mock
    private UserRoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userService, userRepository, credentialRepository, roleRepository,
                passwordEncoder, authenticationManager, jwtService, CLOCK);
    }

    private static RegisterUserRequest registerRequest() {
        return new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Gomez", "3001234567",
                "Santa Marta", LocalDate.of(1995, 5, 10), PASSWORD);
    }

    private static UserResponse andrea() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true, "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(1995, 5, 10));
    }

    // ---------- register ----------

    @Test
    void register_createsCredentialWithBcryptHashAndOnlyRoleUser() {
        User user = User.create("andrea", "andrea@email.com");
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andrea());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("HASH-BCRYPT");

        UserResponse result = service.register(registerRequest());

        assertThat(result).isEqualTo(andrea());

        ArgumentCaptor<UserCredential> credential = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(credential.capture());
        assertThat(credential.getValue().getPasswordHash()).isEqualTo("HASH-BCRYPT").isNotEqualTo(PASSWORD);

        ArgumentCaptor<UserRole> role = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).save(role.capture());
        assertThat(role.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_whenUserServiceFails_noCredentialNorRoleIsCreated() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "username", "andrea"));

        assertThatThrownBy(() -> service.register(registerRequest())).isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(credentialRepository, roleRepository, passwordEncoder);
    }

    // ---------- login ----------

    @Test
    void login_validCredentials_issuesToken() {
        SecurityPrincipal principal = new SecurityPrincipal(5L, "andrea", null, true, List.of(Role.USER));
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(5L)).thenReturn("signed.jwt.token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = service.login(new LoginRequest("  andrea ", PASSWORD));

        assertThat(response).isEqualTo(new AuthResponse("signed.jwt.token", "Bearer", 900L));
    }

    @Test
    void login_wrongPassword_throwsGenericBadCredentials_andIssuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_inactiveUser_looksIdenticalToWrongPassword() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> service.login(new LoginRequest("carlos", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_passwordLongerThan72Bytes_isRejectedWithoutTouchingBcrypt() {
        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "x".repeat(73))))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(authenticationManager, jwtService);
    }

    // ---------- me ----------

    @Test
    void me_returnsMinimalInformation() {
        SecurityPrincipal principal = new SecurityPrincipal(5L, "laura", null, true, List.of(Role.USER, Role.STAFF));

        var response = service.me(principal);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.username()).isEqualTo("laura");
        assertThat(response.roles()).containsExactly("USER", "STAFF");
    }
}
