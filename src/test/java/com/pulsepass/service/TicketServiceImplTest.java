package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.IssueTicketDto;
import com.pulsepass.dto.TicketDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.EventNotSellableException;
import com.pulsepass.exception.InactiveUserException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.exception.UnderAgeException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    // "Ahora" congelado: 2026-09-28
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        // Se construye a mano porque necesita el Clock fijo (mismo orden que los campos de la clase)
        service = new TicketServiceImpl(ticketRepository, userRepository, eventRepository, ticketMapper, CLOCK);
    }

    private Event publishedEvent(int minimumAge) {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("CMF-2026", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), minimumAge, venue);
        event.publish();
        return event;
    }

    private IssueTicketDto request() {
        return new IssueTicketDto("tck-0001", TicketType.GENERAL, new BigDecimal("120000"),
                "andrea@example.com", "cmf-2026");
    }

    @Test
    void shouldIssueTicketAsReserved() {
        User user = User.create("andrea", "andrea@example.com");
        Event event = publishedEvent(0);
        var expected = new TicketDto(1L, "TCK-0001", TicketType.GENERAL, TicketStatus.RESERVED,
                new BigDecimal("120000"), LocalDateTime.now(CLOCK), "andrea@example.com", "CMF-2026");

        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.existsByTicketCode("TCK-0001")).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toDto(any(Ticket.class))).thenReturn(expected);

        TicketDto result = service.issue(request());

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(captor.getValue().getTicketCode()).isEqualTo("TCK-0001");
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issue(request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = User.create("andrea", "andrea@example.com");
        user.deactivate();

        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.issue(request()))
                .isInstanceOf(InactiveUserException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectNonSellableEvent() {
        User user = User.create("andrea", "andrea@example.com");
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event draftEvent = Event.create("CMF-2026", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, venue); // sigue en DRAFT

        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> service.issue(request()))
                .isInstanceOf(EventNotSellableException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicatedTicketCode() {
        User user = User.create("andrea", "andrea@example.com");
        Event event = publishedEvent(0);

        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.existsByTicketCode("TCK-0001")).thenReturn(true);

        assertThatThrownBy(() -> service.issue(request()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnderAgeBuyerWithProfile() {
        User user = User.create("andrea", "andrea@example.com");
        // 15 años el 2026-09-28 con este nacimiento
        user.assignProfile(UserProfile.create("Andrea", "Lopez", "300", "Santa Marta",
                LocalDate.of(2011, 1, 1), user));
        Event event = publishedEvent(18);

        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.issue(request()))
                .isInstanceOf(UnderAgeException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldAllowUnderAgeEventWhenUserHasNoProfile() {
        User user = User.create("andrea", "andrea@example.com"); // sin perfil
        Event event = publishedEvent(18);
        var expected = new TicketDto(1L, "TCK-0001", TicketType.GENERAL, TicketStatus.RESERVED,
                new BigDecimal("120000"), LocalDateTime.now(CLOCK), "andrea@example.com", "CMF-2026");

        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.existsByTicketCode("TCK-0001")).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toDto(any(Ticket.class))).thenReturn(expected);

        TicketDto result = service.issue(request());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldMarkTicketAsPaidThenUsed() {
        User user = User.create("andrea", "andrea@example.com");
        Event event = publishedEvent(0);
        Ticket ticket = Ticket.create("TCK-0001", TicketType.GENERAL, TicketStatus.RESERVED,
                new BigDecimal("120000"), LocalDateTime.now(CLOCK), user, event);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDto(ticket)).thenReturn(null);

        service.pay(1L);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);

        service.markUsed(1L);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    void shouldRejectCancelingAUsedTicket() {
        User user = User.create("andrea", "andrea@example.com");
        Event event = publishedEvent(0);
        Ticket ticket = Ticket.create("TCK-0001", TicketType.GENERAL, TicketStatus.PAID,
                new BigDecimal("120000"), LocalDateTime.now(CLOCK), user, event);
        ticket.markUsed();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(com.pulsepass.exception.InvalidStatusTransitionException.class);
    }
}
