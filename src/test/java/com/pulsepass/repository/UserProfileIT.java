package com.pulsepass.repository;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-USR-001 a FR-USR-004, BR-004, AC-004, UC-04, QT-004, QT-009.
 */
class UserProfileIT extends IntegrationTestBase {

    private User saveUserWithProfile() {
        User user = User.create("andrea", "andrea@example.com");
        user.assignProfile(UserProfile.create(
                "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10), user));
        return userRepository.saveAndFlush(user);
    }

    @Test
    void usuarioConPerfilRecuperaSusDatosDesdeLaRelacionUnoAUno() {
        saveUserWithProfile();
        flushAndClear();

        User found = userRepository.findByEmailIgnoreCase("andrea@example.com").orElseThrow();

        assertTrue(found.isActive());
        assertNotNull(found.getProfile());
        assertEquals("Andrea", found.getProfile().getFirstName());
        assertEquals("Gomez", found.getProfile().getLastName());
        assertEquals("3001234567", found.getProfile().getPhone());
        assertEquals("Santa Marta", found.getProfile().getCity());
        assertEquals(LocalDate.of(1998, 5, 10), found.getProfile().getBirthDate());
        assertEquals(found.getId(), found.getProfile().getUser().getId());
    }

    @Test
    void rechazaUnSegundoPerfilParaElMismoUsuario() {
        Long userId = saveUserWithProfile().getId();

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO user_profiles (first_name, last_name, phone, birth_date, user_id) "
                                + "VALUES (?, ?, ?, ?, ?)",
                        "Otra", "Persona", "3110000000", Date.valueOf(LocalDate.of(2000, 1, 1)), userId));
    }

    @Test
    void buscaUsuarioPorEmailIgnorandoMayusculas() {
        saveUser("carlos", "carlos@example.com");
        flushAndClear();

        User found = userRepository.findByEmailIgnoreCase("CARLOS@EXAMPLE.COM").orElseThrow();

        assertEquals("carlos", found.getUsername());
    }

    @Test
    void rechazaUnEmailDuplicado() {
        saveUser("andrea", "andrea@example.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> saveUser("otra-andrea", "andrea@example.com"));
    }

    @Test
    void rechazaUnUsernameDuplicado() {
        saveUser("andrea", "andrea@example.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> saveUser("andrea", "otro@example.com"));
    }
}