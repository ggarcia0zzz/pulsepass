package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.shared.TextNormalizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Carga identidad, hash, roles y estado del usuario desde la base de datos.
 * Cuenta de consultas constante por request (usuario + roles): sin N+1.
 * No valida reglas comerciales.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository credentialRepository,
                                    UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Login: el identificador es username o email. Un usuario legado sin credencial
     * no puede iniciar sesión (se trata como inexistente).
     */
    @Override
    public SecurityPrincipal loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Invalid identifier");
        }
        String normalized = identifier.trim();

        Optional<User> found = normalized.contains("@")
                ? userRepository.findByEmailIgnoreCase(TextNormalizer.email(normalized))
                : userRepository.findByUsername(normalized);
        User user = found.orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User has no credentials"));

        return toPrincipal(user, credential.getPasswordHash());
    }

    /** Usado por el filtro JWT: revalida estado y roles ACTUALES en cada request. */
    public SecurityPrincipal loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user, null);
    }

    private SecurityPrincipal toPrincipal(User user, String passwordHash) {
        List<Role> roles = roleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRole)
                .toList();
        return new SecurityPrincipal(user.getId(), user.getUsername(), passwordHash, user.isActive(), roles);
    }
}
