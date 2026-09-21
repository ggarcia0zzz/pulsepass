package com.pulsepass.repository;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-EVT-001 a FR-EVT-005, FR-VEN-004, AC-002, AC-006, UC-02, UC-06, QT-003, QT-007, BR-008.
 */
class EventRepositoryIT extends IntegrationTestBase {

    private static final String INSERT_EVENT_SQL =
            "INSERT INTO events (event_code, name, category, status, event_date, venue_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    @Test
    void recuperaUnEventoPorCodigoJuntoASuVenue() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        flushAndClear();

        Event event = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertEquals("CMF-2026", event.getEventCode());
        assertEquals("VEN-SMR-01", event.getVenue().getCode());
    }

    @Test
    void unVenueTieneMuchosEventosYLaConsultaFiltraPorSuCodigo() {
        Venue marina = saveVenue("VEN-SMR-01", "Santa Marta");
        Venue arena = saveVenue("VEN-BOG-01", "Bogota");
        saveEvent("EVT-001", marina, LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED);
        saveEvent("EVT-002", marina, LocalDateTime.now().plusDays(20), EventStatus.DRAFT);
        saveEvent("EVT-003", arena, LocalDateTime.now().plusDays(15), EventStatus.PUBLISHED);
        flushAndClear();

        List<Event> events = eventRepository.findByVenue_Code("VEN-SMR-01");

        Set<String> codes = events.stream().map(Event::getEventCode).collect(Collectors.toSet());
        assertEquals(2, events.size());
        assertEquals(Set.of("EVT-001", "EVT-002"), codes);
    }

    @Test
    void carteleraDevuelveSoloPublicadosOrdenadosPorFechaAscendente() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        saveEvent("EVT-DRAFT", venue, LocalDateTime.now().plusDays(5), EventStatus.DRAFT);
        saveEvent("EVT-LATE", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        saveEvent("EVT-CANCELLED", venue, LocalDateTime.now().plusDays(10), EventStatus.CANCELLED);
        saveEvent("EVT-SOON", venue, LocalDateTime.now().plusDays(15), EventStatus.PUBLISHED);
        flushAndClear();

        List<String> codes = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(Event::getEventCode)
                .toList();

        assertEquals(List.of("EVT-SOON", "EVT-LATE"), codes);
    }

    @Test
    void rechazaUnEventCodeDuplicado() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);

        assertThrows(DataIntegrityViolationException.class,
                () -> saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(40), EventStatus.DRAFT));
    }

    @Test
    void guardaCategoriaYEstadoComoTextoLegibleNoComoOrdinal() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);

        String category = jdbcTemplate.queryForObject(
                "SELECT category FROM events WHERE event_code = ?", String.class, "CMF-2026");
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM events WHERE event_code = ?", String.class, "CMF-2026");

        assertEquals("MUSIC", category);
        assertEquals("PUBLISHED", status);
    }

    @Test
    void laBaseDeDatosRechazaUnEstadoFueraDelCatalogo() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        Long venueId = venue.getId();

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT_EVENT_SQL, "EVT-BAD-STATUS", "Evento", "MUSIC", "ARCHIVED",
                        Timestamp.valueOf(LocalDateTime.now().plusDays(1)), venueId));
    }

    @Test
    void laBaseDeDatosRechazaUnaCategoriaFueraDelCatalogo() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        Long venueId = venue.getId();

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT_EVENT_SQL, "EVT-BAD-CAT", "Evento", "CIRCUS", "DRAFT",
                        Timestamp.valueOf(LocalDateTime.now().plusDays(1)), venueId));
    }

    @Test
    void elEventoQuedaConEdadMinimaYDescripcion() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        flushAndClear();

        Event event = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertTrue(event.getMinimumAge() >= 0);
        assertEquals("Descripcion de CMF-2026", event.getDescription());
    }
}