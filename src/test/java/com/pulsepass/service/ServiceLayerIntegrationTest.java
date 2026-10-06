package com.pulsepass.service;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.ArtistDto;
import com.pulsepass.dto.AssignArtistDto;
import com.pulsepass.dto.ChangeEventStatusDto;
import com.pulsepass.dto.CreateEventDto;
import com.pulsepass.dto.CreateUserProfileDto;
import com.pulsepass.dto.CreateVenueDto;
import com.pulsepass.dto.EventDto;
import com.pulsepass.dto.IssueTicketDto;
import com.pulsepass.dto.RegisterUserDto;
import com.pulsepass.dto.TicketDto;
import com.pulsepass.dto.UserDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.EventNotSellableException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integracion real: Service + MapStruct + Repository + Hibernate + PostgreSQL (Testcontainers).
 * Verifica que los mappers funcionen con entidades reales y que la validacion (@Valid) este activa.
 */
@Testcontainers
@SpringBootTest
@Transactional
class ServiceLayerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Autowired
    private VenueService venueService;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    @Autowired
    private TicketService ticketService;

    @Test
    void fullTicketSalesFlow() {
        venueService.create(new CreateVenueDto("ven-smr-01", "Marina Convention Center",
                "Santa Marta", "Cra 1 # 2-3", 5000));

        EventDto event = eventService.create(new CreateEventDto(
                "cmf-2026", "Caribbean Music Fest 2026", "Festival de musica",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(60), 0, "ven-smr-01"));

        assertThat(event.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.venueCode()).isEqualTo("VEN-SMR-01");

        // Solar Beat ya viene de V2__insert_initial_artists.sql
        ArtistDto solarBeat = artistService.findByStageName("Solar Beat");
        eventService.assignArtist(event.id(), new AssignArtistDto("Solar Beat"));

        EventDto published = eventService.changeStatus(event.id(), new ChangeEventStatusDto(EventStatus.PUBLISHED));
        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(published.artistStageNames()).containsExactly("Solar Beat");

        UserDto andrea = userService.register(new RegisterUserDto("andrea", "Andrea@Example.com"));
        assertThat(andrea.email()).isEqualTo("andrea@example.com");

        userService.createProfile(andrea.id(), new CreateUserProfileDto(
                "Andrea", "Lopez", "3000000000", "Santa Marta", LocalDate.of(1998, 5, 10)));

        TicketDto ticket = ticketService.issue(new IssueTicketDto(
                "tck-0001", TicketType.VIP, new BigDecimal("250000"), "andrea@example.com", "cmf-2026"));

        assertThat(ticket.ticketCode()).isEqualTo("TCK-0001");
        assertThat(ticket.status().name()).isEqualTo("RESERVED");

        TicketDto paid = ticketService.pay(ticket.id());
        assertThat(paid.status().name()).isEqualTo("PAID");

        assertThat(ticketService.countPaidByEvent("CMF-2026")).isEqualTo(1);
        assertThat(ticketService.findPaidByEvent("cmf-2026")).hasSize(1);
        assertThat(ticketService.findByUserEmail("andrea@example.com")).hasSize(1);

        assertThat(eventService.findByArtist("Solar Beat"))
                .extracting(EventDto::eventCode)
                .contains("CMF-2026");
    }

    @Test
    void cannotIssueTicketForANonPublishedEvent() {
        venueService.create(new CreateVenueDto("VEN-1", "Centro", "Santa Marta", "Cra 1", 100));
        EventDto draftEvent = eventService.create(new CreateEventDto(
                "EVT-1", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, "VEN-1"));
        userService.register(new RegisterUserDto("carlos", "carlos@example.com"));

        assertThatThrownBy(() -> ticketService.issue(new IssueTicketDto(
                "TCK-1", TicketType.GENERAL, new BigDecimal("50000"), "carlos@example.com", "EVT-1")))
                .isInstanceOf(EventNotSellableException.class);
    }

    @Test
    void cannotCreateTwoVenuesWithTheSameCode() {
        venueService.create(new CreateVenueDto("VEN-1", "Centro", "Santa Marta", "Cra 1", 100));

        assertThatThrownBy(() -> venueService.create(
                new CreateVenueDto("VEN-1", "Otro Centro", "Bogota", "Cra 2", 200)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void beanValidationRejectsInvalidRequests() {
        assertThatThrownBy(() -> venueService.create(
                new CreateVenueDto(" ", "Centro", "Santa Marta", "Cra 1", 100)))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
