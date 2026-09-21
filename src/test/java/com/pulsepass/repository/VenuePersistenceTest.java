package com.pulsepass.repository;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-VEN-001, FR-VEN-002, FR-VEN-003, AC-001, UC-01, QT-009.
 */
class VenuePersistenceTest extends IntegrationTestBase {

    @Test
    void persisteYRecuperaUnVenuePorIdYPorCodigo() {
        Venue saved = venueRepository.saveAndFlush(
                Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
        flushAndClear();

        Venue byId = venueRepository.findById(saved.getId()).orElseThrow();
        Venue byCode = venueRepository.findByCode("VEN-SMR-01").orElseThrow();

        assertEquals("VEN-SMR-01", byId.getCode());
        assertEquals(saved.getId(), byCode.getId());
        assertEquals("Marina Convention Center", byCode.getName());
        assertTrue(byCode.getCapacity() > 0);
        assertTrue(byCode.isActive());
    }

    @Test
    void rechazaUnCodigoDeVenueDuplicado() {
        venueRepository.saveAndFlush(
                Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
        Venue duplicate = Venue.create("VEN-SMR-01", "Otro venue", "Bogota", "Calle 10 # 5-20", 1000);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(duplicate));
    }

    @Test
    void rechazaCapacidadIgualACero() {
        Venue venue = Venue.create("VEN-ZERO", "Venue sin capacidad", "Santa Marta", "Carrera 1 # 2-3", 0);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(venue));
    }

    @Test
    void rechazaCapacidadNegativa() {
        Venue venue = Venue.create("VEN-NEG", "Venue con capacidad negativa", "Santa Marta", "Carrera 1 # 2-3", -10);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(venue));
    }
}