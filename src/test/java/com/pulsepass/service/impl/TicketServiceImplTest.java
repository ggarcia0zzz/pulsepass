package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.pricing.TicketCodeGenerator;
import com.pulsepass.service.pricing.TicketPriceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Escenario del PRD (seccion 47): venue VEN-SMR-01 con capacidad 3 y evento CMF-2026
 * (minimumAge 18) el 2026-12-05. Usuarios: Andrea (25), Carlos (21), Laura (17), Miguel (inactivo).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    // "Ahora" congelado: 2026-09-28
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 5, 20, 0);

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCodeGenerator codeGenerator;

    // Real (no mock): la estrategia de precios es parte de lo que se prueba. Base = 100000
    private final TicketPriceCalculator priceCalculator = new TicketPriceCalculator(new BigDecimal("100000.00"));

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(ticketRepository, userRepository, eventRepository, ticketMapper,
                priceCalculator, codeGenerator, CLOCK);
    }

    // ---------- helpers ----------

    private User userBornOn(String username, String email, LocalDate birthDate) {
        User user = User.create(username, email);
        user.assignProfile(UserProfile.create(username, "Test", "3000000000", "Santa Marta", birthDate, user));
        return user;
    }

    private User andrea() {   // 25 anos al momento del evento
        return userBornOn("andrea", "andrea@email.com", LocalDate.of(2001, 3, 10));
    }

    private Event event(EventStatus status, LocalDateTime date, int minimumAge, int capacity) {
        Venue venue = Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 2-3", capacity);
        Event event = Event.create("CMF-2026", "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                date, minimumAge, venue);
        switch (status) {
            case DRAFT -> { /* estado inicial */ }
            case PUBLISHED -> event.publish();
            case SOLD_OUT -> {
                event.publish();
                event.markSoldOut();
            }
            case CANCELLED -> event.cancel();
            case FINISHED -> {
                event.publish();
                event.finish();
            }
        }
        return event;
    }

    private Event publishedEvent() {
        return event(EventStatus.PUBLISHED, EVENT_DATE, 18, 3);
    }

    private PurchaseTicketRequest request(String email, TicketType type) {
        return new PurchaseTicketRequest(email, "cmf-2026", type);
    }

    private TicketResponse responseOf(TicketStatus status) {
        return new TicketResponse(1L, "TCK-TEST0001", TicketType.VIP, new BigDecimal("250000.00"), status,
                NOW, "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    private Ticket ticket(TicketStatus status, Event event) {
        return Ticket.create("TCK-0001", TicketType.GENERAL, status, new BigDecimal("100000.00"),
                NOW, andrea(), event);
    }

    /** Stubs del camino feliz hasta la validacion de capacidad (count = tickets PAID existentes). */
    private void givenPurchaseReadyFor(User user, Event event, long paidTickets) {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(paidTickets);
    }

    private void givenTicketCanBeSaved() {
        when(codeGenerator.next()).thenReturn("TCK-TEST0001");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(responseOf(TicketStatus.PAID));
    }

    // =====================================================================
    // purchase
    // =====================================================================

    @Test
    void purchase_validRequest_savesPaidTicketWithSystemCalculatedPrice() {
        // ARRANGE (TEST-TICKET-001 / AC-004)
        User andrea = andrea();
        Event event = publishedEvent();
        givenPurchaseReadyFor(andrea, event, 0);
        givenTicketCanBeSaved();

        // ACT (Andrea compra VIP; el email llega con mayusculas y espacios)
        TicketResponse result = service.purchase(request(" Andrea@Email.com ", TicketType.VIP));

        // ASSERT
        assertThat(result).isEqualTo(responseOf(TicketStatus.PAID));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getType()).isEqualTo(TicketType.VIP);
        assertThat(saved.getPrice()).isEqualByComparingTo("250000.00");   // VIP = 2.5 x base
        assertThat(saved.getTicketCode()).isEqualTo("TCK-TEST0001");
        assertThat(saved.getPurchaseDate()).isEqualTo(NOW);
        assertThat(saved.getUser()).isSameAs(andrea);
        assertThat(saved.getEvent()).isSameAs(event);

        // No era el ultimo cupo: el evento sigue PUBLISHED y no se vuelve a guardar
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void purchase_secondUserAlsoCanBuy() {
        // ARRANGE (AC-005: Carlos, 21 anos)
        User carlos = userBornOn("carlos", "carlos@email.com", LocalDate.of(2005, 6, 15));
        Event event = publishedEvent();
        givenPurchaseReadyFor(carlos, event, 1);
        givenTicketCanBeSaved();

        // ACT
        TicketResponse result = service.purchase(request("carlos@email.com", TicketType.GENERAL));

        // ASSERT
        assertThat(result).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void purchase_unknownUser_throwsResourceNotFoundAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-002)
        when(userRepository.findByEmailIgnoreCase("nadie@email.com")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("nadie@email.com", TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nadie@email.com");

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void purchase_inactiveUser_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-003 / AC-007: Miguel esta inactivo)
        User miguel = userBornOn("miguel", "miguel@email.com", LocalDate.of(1996, 1, 1));
        miguel.deactivate();
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(miguel));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("miguel@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Inactive user");

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void purchase_unknownEvent_throwsResourceNotFoundAndDoesNotSave() {
        // ARRANGE (BR-TICKET-003)
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea()));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CMF-2026");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_draftEvent_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-004)
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea()));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event(EventStatus.DRAFT, EVENT_DATE, 18, 3)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_cancelledEvent_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-005)
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea()));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event(EventStatus.CANCELLED, EVENT_DATE, 18, 3)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        verify(ticketRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"SOLD_OUT", "FINISHED"})
    void purchase_soldOutOrFinishedEvent_throwsBusinessRuleAndDoesNotSave(EventStatus status) {
        // ARRANGE (BR-TICKET-004)
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea()));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event(status, EVENT_DATE, 18, 3)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(status.name());

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_eventAlreadyHappened_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (BR-TICKET-005)
        Event past = event(EventStatus.PUBLISHED, LocalDateTime.of(2026, 9, 1, 20, 0), 18, 3);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea()));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(past));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already taken place");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_underageUser_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-006 / AC-006: Laura tiene 17 el dia del evento)
        User laura = userBornOn("laura", "laura@email.com", LocalDate.of(2009, 8, 20));
        when(userRepository.findByEmailIgnoreCase("laura@email.com")).thenReturn(Optional.of(laura));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent()));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("laura@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");

        verify(ticketRepository, never()).save(any());
        verify(ticketRepository, never()).countByEventCodeAndStatus(any(), any());
    }

    @Test
    void purchase_ageIsEvaluatedAtEventDateNotToday() {
        // ARRANGE: nace el 2008-10-01 -> hoy (2026-09-28) tiene 17, pero el 2026-12-05 ya tiene 18
        User turnsEighteenBeforeEvent = userBornOn("sofia", "sofia@email.com", LocalDate.of(2008, 10, 1));
        Event event = publishedEvent();
        givenPurchaseReadyFor(turnsEighteenBeforeEvent, event, 0);
        givenTicketCanBeSaved();

        // ACT
        TicketResponse result = service.purchase(request("sofia@email.com", TicketType.GENERAL));

        // ASSERT
        assertThat(result).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void purchase_userWithoutProfileOnAgeRestrictedEvent_throwsBusinessRule() {
        // ARRANGE: sin fecha de nacimiento no se puede comprobar la edad
        User noProfile = User.create("andrea", "andrea@email.com");
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(noProfile));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent()));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("profile");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_eventWithoutAgeRestriction_doesNotRequireProfile() {
        // ARRANGE (minimumAge = 0 -> sin restriccion)
        User noProfile = User.create("andrea", "andrea@email.com");
        Event event = event(EventStatus.PUBLISHED, EVENT_DATE, 0, 3);
        givenPurchaseReadyFor(noProfile, event, 0);
        givenTicketCanBeSaved();

        // ACT
        TicketResponse result = service.purchase(request("andrea@email.com", TicketType.GENERAL));

        // ASSERT
        assertThat(result).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void purchase_noCapacityLeft_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-007 / AC-009: cuarta compra con capacidad 3 y 3 PAID)
        Event event = publishedEvent();
        givenPurchaseReadyFor(andrea(), event, 3);

        // ACT + ASSERT (BR-TICKET-007)
        assertThatThrownBy(() -> service.purchase(request("andrea@email.com", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no capacity");

        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void purchase_lastAvailableTicket_savesTicketAndMarksEventSoldOut() {
        // ARRANGE (TEST-TICKET-008 / AC-008: capacidad 3, ya hay 2 PAID)
        Event event = publishedEvent();
        givenPurchaseReadyFor(andrea(), event, 2);
        givenTicketCanBeSaved();
        when(eventRepository.save(event)).thenReturn(event);

        // ACT
        TicketResponse result = service.purchase(request("andrea@email.com", TicketType.GENERAL));

        // ASSERT (BR-TICKET-008: ticket guardado y evento SOLD_OUT en la misma operacion)
        assertThat(result).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(eq(event));
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    // =====================================================================
    // cancel
    // =====================================================================

    @Test
    void cancel_paidTicket_becomesCancelled() {
        // ARRANGE (TEST-TICKET-009)
        Ticket paid = ticket(TicketStatus.PAID, publishedEvent());
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paid));
        when(ticketRepository.save(paid)).thenReturn(paid);
        when(ticketMapper.toResponse(paid)).thenReturn(responseOf(TicketStatus.CANCELLED));

        // ACT
        TicketResponse result = service.cancel("tck-0001");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(paid.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(paid);
    }

    @Test
    void cancel_usedTicket_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-010 / AC-011)
        Ticket used = ticket(TicketStatus.PAID, publishedEvent());
        used.markUsed();
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(used));

        // ACT + ASSERT (BR-TICKET-011)
        assertThatThrownBy(() -> service.cancel("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("USED");

        assertThat(used.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_alreadyCancelledTicket_throwsBusinessRule() {
        // ARRANGE (BR-TICKET-011)
        Ticket cancelled = ticket(TicketStatus.PAID, publishedEvent());
        cancelled.cancel();
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(cancelled));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_reservedTicket_throwsBusinessRule() {
        // ARRANGE (BR-TICKET-010: solo PAID se cancela)
        Ticket reserved = ticket(TicketStatus.RESERVED, publishedEvent());
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(reserved));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID");

        assertThat(reserved.getStatus()).isEqualTo(TicketStatus.RESERVED);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_afterEventDate_throwsBusinessRule() {
        // ARRANGE (BR-TICKET-012: el evento fue el 2026-09-01 y hoy es 2026-09-28)
        Event past = event(EventStatus.PUBLISHED, LocalDateTime.of(2026, 9, 1, 20, 0), 18, 3);
        Ticket paid = ticket(TicketStatus.PAID, past);
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paid));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after the event date");

        assertThat(paid.getStatus()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_unknownTicket_throwsResourceNotFound() {
        // ARRANGE
        when(ticketRepository.findByTicketCode("TCK-XXXX")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel("TCK-XXXX"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // =====================================================================
    // markAsUsed
    // =====================================================================

    @Test
    void markAsUsed_paidTicket_becomesUsed() {
        // ARRANGE (TEST-TICKET-011 / AC-010)
        Ticket paid = ticket(TicketStatus.PAID, publishedEvent());
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paid));
        when(ticketRepository.save(paid)).thenReturn(paid);
        when(ticketMapper.toResponse(paid)).thenReturn(responseOf(TicketStatus.USED));

        // ACT
        TicketResponse result = service.markAsUsed("TCK-0001");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.USED);
        assertThat(paid.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(paid);
    }

    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-TICKET-012 / BR-TICKET-014)
        Ticket cancelled = ticket(TicketStatus.PAID, publishedEvent());
        cancelled.cancel();
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(cancelled));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.markAsUsed("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        assertThat(cancelled.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void markAsUsed_alreadyUsedTicket_throwsBusinessRule() {
        // ARRANGE (BR-TICKET-013: solo PAID)
        Ticket used = ticket(TicketStatus.PAID, publishedEvent());
        used.markUsed();
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(used));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.markAsUsed("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID");

        verify(ticketRepository, never()).save(any());
    }

    // =====================================================================
    // consultas
    // =====================================================================

    @Test
    void findByCode_existingTicket_returnsResponse() {
        // ARRANGE
        Ticket paid = ticket(TicketStatus.PAID, publishedEvent());
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paid));
        when(ticketMapper.toResponse(paid)).thenReturn(responseOf(TicketStatus.PAID));

        // ACT
        TicketResponse result = service.findByCode(" tck-0001 ");

        // ASSERT
        assertThat(result).isEqualTo(responseOf(TicketStatus.PAID));
    }

    @Test
    void findByCode_unknownTicket_throwsResourceNotFound() {
        // ARRANGE
        when(ticketRepository.findByTicketCode("TCK-XXXX")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByCode("TCK-XXXX"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found: TCK-XXXX");
    }

    @Test
    void findByUserEmail_returnsTicketsOfThatUser() {
        // ARRANGE
        Ticket paid = ticket(TicketStatus.PAID, publishedEvent());
        when(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("andrea@email.com"))
                .thenReturn(List.of(paid));
        when(ticketMapper.toResponse(paid)).thenReturn(responseOf(TicketStatus.PAID));

        // ACT
        List<TicketResponse> result = service.findByUserEmail("Andrea@Email.com");

        // ASSERT
        assertThat(result).containsExactly(responseOf(TicketStatus.PAID));
    }

    @Test
    void findPaidTicketsByEvent_queriesPaidStatus() {
        // ARRANGE
        Ticket paid = ticket(TicketStatus.PAID, publishedEvent());
        when(ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .thenReturn(List.of(paid));
        when(ticketMapper.toResponse(paid)).thenReturn(responseOf(TicketStatus.PAID));

        // ACT
        List<TicketResponse> result = service.findPaidTicketsByEvent("cmf-2026");

        // ASSERT
        assertThat(result).containsExactly(responseOf(TicketStatus.PAID));
        verify(ticketRepository).findByEvent_EventCodeAndStatus(eq("CMF-2026"), eq(TicketStatus.PAID));
    }
}
