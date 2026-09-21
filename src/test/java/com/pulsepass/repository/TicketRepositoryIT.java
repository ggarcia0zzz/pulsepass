package com.pulsepass.repository;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-TKT-001 a FR-TKT-008, FR-SRC-004, BR-005 a BR-007, AC-005, AC-008, UC-05, UC-08,
 * QT-006, QT-007, QT-008, QT-009.
 *
 * Escenario (sección 16 del PRD): evento CMF-2026 en VEN-SMR-01 con 4 tickets:
 * Andrea VIP PAID 250000, Carlos GENERAL PAID 120000,
 * Laura GENERAL RESERVED 120000, Miguel VIP CANCELLED 250000.
 */
class TicketRepositoryIT extends IntegrationTestBase {

    private static final String INSERT_TICKET_SQL =
            "INSERT INTO tickets (ticket_code, type, status, price, purchase_date, user_id, event_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    @BeforeEach
    void crearEscenarioDelPrd() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        Event event = saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        User andrea = saveUser("andrea", "andrea@example.com");
        User carlos = saveUser("carlos", "carlos@example.com");
        User laura = saveUser("laura", "laura@example.com");
        User miguel = saveUser("miguel", "miguel@example.com");

        saveTicket("TCK-0001", TicketType.VIP, TicketStatus.PAID, "250000", andrea, event);
        saveTicket("TCK-0002", TicketType.GENERAL, TicketStatus.PAID, "120000", carlos, event);
        saveTicket("TCK-0003", TicketType.GENERAL, TicketStatus.RESERVED, "120000", laura, event);
        saveTicket("TCK-0004", TicketType.VIP, TicketStatus.CANCELLED, "250000", miguel, event);
        flushAndClear();
    }

    @Test
    void elTicketPerteneceAUnUsuarioYAUnEvento() {
        Ticket ticket = ticketRepository.findByTicketCode("TCK-0001").orElseThrow();

        assertEquals("andrea@example.com", ticket.getUser().getEmail());
        assertEquals("CMF-2026", ticket.getEvent().getEventCode());
        assertEquals(TicketType.VIP, ticket.getType());
        assertEquals(TicketStatus.PAID, ticket.getStatus());
        assertEquals(0, new BigDecimal("250000").compareTo(ticket.getPrice()));
    }

    @Test
    void consultaTicketsDeUnUsuarioPorEmailYOpcionalmentePorEstado() {
        User andrea = userRepository.findByEmailIgnoreCase("andrea@example.com").orElseThrow();
        Event event = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        saveTicket("TCK-0005", TicketType.GENERAL, TicketStatus.RESERVED, "120000", andrea, event);
        flushAndClear();

        List<Ticket> all = ticketRepository.findByUser_Email("andrea@example.com");
        List<Ticket> paid = ticketRepository.findByUser_EmailAndStatus("andrea@example.com", TicketStatus.PAID);
        List<Ticket> reserved = ticketRepository.findByUser_EmailAndStatus("andrea@example.com", TicketStatus.RESERVED);
        List<Ticket> cancelled = ticketRepository.findByUser_EmailAndStatus("andrea@example.com", TicketStatus.CANCELLED);

        assertEquals(2, all.size());
        assertEquals(1, paid.size());
        assertEquals("TCK-0001", paid.get(0).getTicketCode());
        assertEquals(1, reserved.size());
        assertEquals("TCK-0005", reserved.get(0).getTicketCode());
        assertEquals(0, cancelled.size());
    }

    @Test
    void recuperaSoloLosTicketsPagadosDeUnEvento() {
        List<Ticket> paid = ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID);

        Set<String> codes = paid.stream().map(Ticket::getTicketCode).collect(Collectors.toSet());
        assertEquals(Set.of("TCK-0001", "TCK-0002"), codes);
    }

    @Test
    void cuentaSoloLosTicketsPagadosDeUnEvento() {
        long paid = ticketRepository.countByEventCodeAndStatus("CMF-2026", TicketStatus.PAID);
        long reserved = ticketRepository.countByEventCodeAndStatus("CMF-2026", TicketStatus.RESERVED);
        long cancelled = ticketRepository.countByEventCodeAndStatus("CMF-2026", TicketStatus.CANCELLED);

        assertEquals(2L, paid);
        assertEquals(1L, reserved);
        assertEquals(1L, cancelled);
    }

    @Test
    void rechazaUnTicketCodeDuplicado() {
        User carlos = userRepository.findByUsername("carlos").orElseThrow();
        Event event = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThrows(DataIntegrityViolationException.class,
                () -> saveTicket("TCK-0001", TicketType.GENERAL, TicketStatus.RESERVED, "120000", carlos, event));
    }

    @Test
    void rechazaUnPrecioNegativo() {
        User carlos = userRepository.findByUsername("carlos").orElseThrow();
        Event event = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThrows(DataIntegrityViolationException.class,
                () -> saveTicket("TCK-0009", TicketType.GENERAL, TicketStatus.RESERVED, "-1", carlos, event));
    }

    @Test
    void laBaseDeDatosRechazaUnTicketSinUsuarioValido() {
        Long eventId = eventRepository.findByEventCode("CMF-2026").orElseThrow().getId();

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT_TICKET_SQL, "TCK-0010", "GENERAL", "RESERVED",
                        new BigDecimal("100000"), Timestamp.valueOf(LocalDateTime.now()), -1L, eventId));
    }

    @Test
    void laBaseDeDatosRechazaUnTicketSinEventoValido() {
        Long userId = userRepository.findByUsername("carlos").orElseThrow().getId();

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT_TICKET_SQL, "TCK-0011", "GENERAL", "RESERVED",
                        new BigDecimal("100000"), Timestamp.valueOf(LocalDateTime.now()), userId, -1L));
    }

    @Test
    void consultaTicketsDeEventosFuturosEnOrdenCronologico() {
        Venue venue = venueRepository.findByCode("VEN-SMR-01").orElseThrow();
        User andrea = userRepository.findByUsername("andrea").orElseThrow();
        Event past = saveEvent("EVT-PAST", venue, LocalDateTime.now().minusDays(5), EventStatus.PUBLISHED);
        Event near = saveEvent("EVT-NEAR", venue, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);
        saveTicket("TCK-0101", TicketType.GENERAL, TicketStatus.PAID, "120000", andrea, past);
        saveTicket("TCK-0102", TicketType.GENERAL, TicketStatus.PAID, "120000", andrea, near);
        flushAndClear();

        List<String> codes = ticketRepository.findByEventDateAfter(LocalDateTime.now()).stream()
                .map(Ticket::getTicketCode)
                .toList();

        // 1 ticket del evento cercano (+5 dias) y los 4 del escenario (+30 dias)
        assertEquals(5, codes.size());
        assertEquals("TCK-0102", codes.get(0));
        assertFalse(codes.contains("TCK-0101"));
    }
}