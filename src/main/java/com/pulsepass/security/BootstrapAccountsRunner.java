package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.shared.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Semilla LOCAL controlada de cuentas privilegiadas (ADMIN y STAFF) para desarrollo.
 * Solo se activa con el perfil "dev". La contraseña llega por variable de entorno
 * (PULSEPASS_BOOTSTRAP_ADMIN_PASSWORD / PULSEPASS_BOOTSTRAP_STAFF_PASSWORD); sin contraseña no se crea
 * nada: no existen contraseñas por defecto ni hashes versionados. Los roles privilegiados nunca se
 * asignan por parámetros HTTP.
 */
@Component
@Profile("dev")
public class BootstrapAccountsRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAccountsRunner.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String staffUsername;
    private final String staffEmail;
    private final String staffPassword;

    public BootstrapAccountsRunner(
            UserRepository userRepository,
            UserCredentialRepository credentialRepository,
            UserRoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${pulsepass.bootstrap.admin.username:admin}") String adminUsername,
            @Value("${pulsepass.bootstrap.admin.email:admin@pulsepass.local}") String adminEmail,
            @Value("${pulsepass.bootstrap.admin.password:}") String adminPassword,
            @Value("${pulsepass.bootstrap.staff.username:staff}") String staffUsername,
            @Value("${pulsepass.bootstrap.staff.email:staff@pulsepass.local}") String staffEmail,
            @Value("${pulsepass.bootstrap.staff.password:}") String staffPassword) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.staffUsername = staffUsername;
        this.staffEmail = staffEmail;
        this.staffPassword = staffPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureAccount(adminUsername, adminEmail, adminPassword, Set.of(Role.USER, Role.ADMIN));
        ensureAccount(staffUsername, staffEmail, staffPassword, Set.of(Role.USER, Role.STAFF));
    }

    private void ensureAccount(String username, String email, String password, Set<Role> roles) {
        if (password == null || password.isBlank()) {
            log.info("Bootstrap account '{}' skipped: no password provided in the environment", username);
            return;
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Bootstrap password for '" + username + "' must have 12+ characters and at most 72 bytes");
        }
        String normalizedEmail = TextNormalizer.email(email);
        if (userRepository.existsByUsername(username) || userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.info("Bootstrap account '{}' already exists; left untouched", username);
            return;
        }

        User user = userRepository.save(User.create(username, normalizedEmail));
        credentialRepository.save(UserCredential.create(user, passwordEncoder.encode(password), LocalDateTime.now(clock)));
        roles.forEach(role -> roleRepository.save(UserRole.of(user.getId(), role)));
        log.info("Bootstrap account '{}' created with roles {}", username, roles);
    }
}
