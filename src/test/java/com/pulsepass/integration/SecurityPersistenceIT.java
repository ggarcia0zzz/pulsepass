package com.pulsepass.integration;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Valida la migración V4 y el mapeo de credenciales/roles contra PostgreSQL real. */
class SecurityPersistenceIT extends IntegrationTestBase {

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private UserRoleRepository roleRepository;

    @Test
    @DisplayName("V4: las tablas user_credentials y user_roles existen")
    void v4Tables_exist() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name IN ('user_credentials', 'user_roles')
                """, Integer.class);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("La credencial se persiste ligada al ID del usuario y el hash se relee intacto")
    void credential_isPersistedWithUserId() {
        User user = saveUser("andrea", "andrea@email.com");

        credentialRepository.saveAndFlush(UserCredential.create(user, "$2a$10$hash-ficticio", LocalDateTime.now()));
        flushAndClear();

        UserCredential loaded = credentialRepository.findById(user.getId()).orElseThrow();
        assertThat(loaded.getUserId()).isEqualTo(user.getId());
        assertThat(loaded.getPasswordHash()).isEqualTo("$2a$10$hash-ficticio");
    }

    @Test
    @DisplayName("Un usuario puede tener varios roles; el mismo rol dos veces viola la PK compuesta")
    void roles_compositeKey() {
        User user = saveUser("laura", "laura@email.com");
        roleRepository.saveAndFlush(UserRole.of(user.getId(), Role.USER));
        roleRepository.saveAndFlush(UserRole.of(user.getId(), Role.STAFF));
        flushAndClear();

        assertThat(roleRepository.findByUserId(user.getId())).extracting(UserRole::getRole)
                .containsExactlyInAnyOrder(Role.USER, Role.STAFF);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, 'STAFF')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("El CHECK rechaza roles fuera de USER/STAFF/ADMIN")
    void roles_checkConstraint() {
        User user = saveUser("carlos", "carlos@email.com");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, 'SUPERUSER')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La credencial exige un usuario existente (FK)")
    void credential_requiresExistingUser() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_credentials (user_id, password_hash) VALUES (999999, 'x')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
