package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.Artist;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base de las pruebas de integración (NFR-004 y NFR-005).
 *
 * - Levanta PostgreSQL real con Testcontainers (nada de H2).
 * - Flyway construye el esquema (V1, V2, V3) y Hibernate solo lo valida.
 * - Cada prueba corre dentro de una transacción que se revierte al terminar,
 *   así que las pruebas no se contaminan entre sí.
 *
 * Nota: los 5 artistas iniciales vienen de V2 y existen en toda prueba.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected VenueRepository venueRepository;

    @Autowired
    protected EventRepository eventRepository;

    @Autowired
    protected ArtistRepository artistRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TicketRepository ticketRepository;

    // Solo para verificar restricciones de la BD con SQL directo dentro de las pruebas.
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @PersistenceContext
    protected EntityManager entityManager;

    /** Envía todo a la BD y limpia la caché de Hibernate para forzar lecturas reales. */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected Venue saveVenue(String code, String city) {
        return venueRepository.saveAndFlush(
                Venue.create(code, "Venue " + code, city, "Carrera 1 # 2-3", 5000));
    }

    /** Solo soporta los estados DRAFT (por defecto), PUBLISHED y CANCELLED. */
    protected Event saveEvent(String eventCode, Venue venue, LocalDateTime date, EventStatus status) {
        Event event = Event.create(eventCode, "Evento " + eventCode, "Descripcion de " + eventCode,
                EventCategory.MUSIC, date, 18, venue);
        switch (status) {
            case PUBLISHED -> event.publish();
            case CANCELLED -> event.cancel();
            default -> {
                // DRAFT: estado inicial
            }
        }
        return eventRepository.saveAndFlush(event);
    }

    /** Recupera uno de los artistas cargados por V2 (Solar Beat, Neon Waves, etc.). */
    protected Artist artist(String stageName) {
        return artistRepository.findByStageName(stageName).orElseThrow();
    }

    protected User saveUser(String username, String email) {
        return userRepository.saveAndFlush(User.create(username, email));
    }

    protected Ticket saveTicket(String ticketCode, TicketType type, TicketStatus status,
                                String price, User user, Event event) {
        return ticketRepository.saveAndFlush(
                Ticket.create(ticketCode, type, status, new BigDecimal(price),
                        LocalDateTime.now(), user, event));
    }
}