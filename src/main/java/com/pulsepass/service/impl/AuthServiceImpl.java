package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String TOKEN_TYPE = "Bearer";
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthServiceImpl(UserService userService,
                           UserRepository userRepository,
                           UserCredentialRepository credentialRepository,
                           UserRoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           Clock clock) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * Atómico: UserService.register() es @Transactional (REQUIRED) y se une a esta transacción, de modo que
     * User + perfil + credencial + rol se confirman juntos o se revierten juntos (NFR-SEC004).
     * UserService sigue aplicando unicidad y reglas de negocio sin modificarse.
     */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse created = userService.register(request);

        User user = userRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("User was not persisted: " + created.id()));

        credentialRepository.save(
                UserCredential.create(user, passwordEncoder.encode(request.password()), LocalDateTime.now(clock)));

        // El registro público SIEMPRE crea ROLE_USER. Nunca acepta roles del cliente (NFR-SEC009).
        roleRepository.save(UserRole.of(user.getId(), Role.USER));

        return created;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
                throw new BadCredentialsException("Invalid credentials");
            }

            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));

            SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(principal.getId());

            log.info("Login successful userId={}", principal.getId());
            return new AuthResponse(token, TOKEN_TYPE, jwtService.accessTokenTtlSeconds());

        } catch (AuthenticationException ex) {
            // Mismo 401 para contraseña incorrecta, usuario inexistente, sin credenciales o inactivo:
            // no se revela si el usuario existe (FR-SECAUTH-003). Se registra el hecho sin credenciales.
            log.warn("Login failed: {}", ex.getClass().getSimpleName());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Override
    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        return new AuthenticatedUserResponse(principal.getId(), principal.getUsername(), principal.roleNames());
    }
}
