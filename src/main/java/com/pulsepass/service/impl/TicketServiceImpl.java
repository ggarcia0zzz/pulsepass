package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.pricing.TicketCodeGenerator;
import com.pulsepass.service.pricing.TicketPriceCalculator;
import com.pulsepass.shared.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator priceCalculator;
    private final TicketCodeGenerator codeGenerator;
    private final Clock clock;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketMapper ticketMapper,
                             TicketPriceCalculator priceCalculator,
                             TicketCodeGenerator codeGenerator,
                             Clock clock) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.priceCalculator = priceCalculator;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    /**
     * Compra atomica (seccion 39 del PRD): usuario + evento + edad + capacidad + precio +
     * ticket + SOLD_OUT ocurren en una sola transaccion. Si cualquier paso lanza una
     * excepcion (RuntimeException) se hace ROLLBACK de todo.
     */
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {

        // BR-TICKET-001: el usuario debe existir
        String email = TextNormalizer.email(request.userEmail());
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // BR-TICKET-002: un usuario inactivo no puede comprar
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive user cannot purchase tickets: " + user.getEmail());
        }

        // BR-TICKET-003: el evento debe existir
        String eventCode = TextNormalizer.code(request.eventCode());
        Event event = eventRepository
                .findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventCode));

        // BR-TICKET-004: solo se compra cuando event.status == PUBLISHED
        // (DRAFT, SOLD_OUT, CANCELLED y FINISHED no venden)
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets cannot be purchased for event %s because it is %s"
                            .formatted(event.getEventCode(), event.getStatus()));
        }

        // BR-TICKET-005: no se compra para un evento que ya ocurrio
        LocalDateTime now = LocalDateTime.now(clock);
        if (!event.getEventDate().isAfter(now)) {
            throw new BusinessRuleException(
                    "Event %s has already taken place".formatted(event.getEventCode()));
        }

        // BR-TICKET-006: edad minima evaluada en la FECHA DEL EVENTO
        validateMinimumAge(user, event);

        // BR-TICKET-007: paidTickets < venue.capacity
        int capacity = event.getVenue().getCapacity();
        long paidTickets = ticketRepository.countByEventCodeAndStatus(
                event.getEventCode(), TicketStatus.PAID);
        if (paidTickets >= capacity) {
            throw new BusinessRuleException(
                    "Event %s has no capacity left (%d/%d)"
                            .formatted(event.getEventCode(), paidTickets, capacity));
        }

        // BR-TICKET-009: precio calculado por el sistema, nunca negativo
        BigDecimal price = priceCalculator.calculate(request.type());

        // Version simplificada: toda compra valida genera un ticket PAID
        Ticket ticket = Ticket.create(
                codeGenerator.next(),
                request.type(),
                TicketStatus.PAID,
                price,
                now,
                user,
                event
        );
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra completa la capacidad -> SOLD_OUT en la misma transaccion
        if (paidTickets + 1 == capacity) {
            event.markSoldOut();
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(findTicket(ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository
                .findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(TextNormalizer.email(email))
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository
                .findByEvent_EventCodeAndStatus(TextNormalizer.code(eventCode), TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {

        Ticket ticket = findTicket(ticketCode);

        // BR-TICKET-011: USED y CANCELLED no se pueden cancelar
        if (ticket.getStatus() == TicketStatus.USED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Ticket %s is %s and cannot be cancelled"
                            .formatted(ticket.getTicketCode(), ticket.getStatus()));
        }

        // BR-TICKET-010: solo se cancela un ticket PAID
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Ticket %s is %s"
                            .formatted(ticket.getTicketCode(), ticket.getStatus()));
        }

        // BR-TICKET-012: no se cancela despues de la fecha del evento
        if (LocalDateTime.now(clock).isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException(
                    "Ticket %s cannot be cancelled after the event date".formatted(ticket.getTicketCode()));
        }

        ticket.cancel(); // PAID -> CANCELLED

        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {

        Ticket ticket = findTicket(ticketCode);

        // BR-TICKET-014: un ticket CANCELLED nunca puede utilizarse
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Ticket %s is CANCELLED and cannot be used".formatted(ticket.getTicketCode()));
        }

        // BR-TICKET-013: solo se marca como usado un ticket PAID
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be used. Ticket %s is %s"
                            .formatted(ticket.getTicketCode(), ticket.getStatus()));
        }

        ticket.markUsed(); // PAID -> USED

        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    private void validateMinimumAge(User user, Event event) {
        int minimumAge = event.getMinimumAge() == null ? 0 : event.getMinimumAge();
        if (minimumAge <= 0) {
            return; // 0 = sin restriccion de edad
        }

        // Sin fecha de nacimiento no se puede comprobar la edad: se rechaza la compra
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new BusinessRuleException(
                    "User profile with birth date is required to verify the minimum age of event "
                            + event.getEventCode());
        }

        int ageAtEventDate = Period.between(profile.getBirthDate(), event.getEventDate().toLocalDate()).getYears();
        if (ageAtEventDate < minimumAge) {
            throw new BusinessRuleException(
                    "User does not meet minimum age: %d required, %d at event date"
                            .formatted(minimumAge, ageAtEventDate));
        }
    }

    private Ticket findTicket(String ticketCode) {
        String normalized = TextNormalizer.code(ticketCode);

        return ticketRepository
                .findByTicketCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", normalized));
    }
}
