package com.pulsepass.migration;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFR-002, NFR-003, QT-001, QT-002, FR-EVT-006.
 */
class FlywayMigrationIT extends IntegrationTestBase {

    @Autowired
    private Flyway flyway;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @Test
    void flywayAplicaV1V2yV3DesdeUnaBaseVacia() {
        MigrationInfo[] applied = flyway.info().applied();

        List<String> versions = Arrays.stream(applied)
                .map(migration -> migration.getVersion().getVersion())
                .toList();

        assertEquals(List.of("1", "2", "3"), versions);
        for (MigrationInfo migration : applied) {
            assertEquals(MigrationState.SUCCESS, migration.getState());
        }
    }

    @Test
    void hibernateSoloValidaElEsquema() {
        assertEquals("validate", ddlAuto);
    }

    @Test
    void v2InsertaElCatalogoInicialDeArtistas() {
        List<String> initialArtists = List.of(
                "Solar Beat", "Neon Waves", "Caribbean Sound", "Ocean Drive", "Digital Pulse");

        for (String stageName : initialArtists) {
            assertTrue(artistRepository.findByStageName(stageName).isPresent(),
                    "Falta el artista inicial: " + stageName);
        }
    }

    @Test
    void v3AgregaStreamingUrlOpcionalAEventos() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        var hybrid = saveEvent("EVT-HYBRID", venue, LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED);
        hybrid.setStreamingUrl("https://stream.pulsepass.example/evt-hybrid");
        saveEvent("EVT-INPERSON", venue, LocalDateTime.now().plusDays(11), EventStatus.PUBLISHED);
        flushAndClear();

        assertEquals("https://stream.pulsepass.example/evt-hybrid",
                eventRepository.findByEventCode("EVT-HYBRID").orElseThrow().getStreamingUrl());
        assertNull(eventRepository.findByEventCode("EVT-INPERSON").orElseThrow().getStreamingUrl());
    }
}