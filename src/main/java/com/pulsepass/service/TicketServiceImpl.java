package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.enums.TicketStatus;
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
import com.pulsepass.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class TicketServiceImpl implements TicketService {

    // OJO: el orden de estos campos define el orden del constructor (@RequiredArgsConstructor)
    private final TicketRepository ticketRepository;

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

    private final TicketMapper ticketMapper;

    private final Clock clock;

    @Override
    @Transactional
    public TicketDto issue(IssueTicketDto request) {

        User user = userRepository
                .findByEmailIgnoreCase(TextNormalizer.email(request.userEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userEmail()));

        // Regla derivada: un usuario inactivo no puede comprar/reservar tickets
        if (!user.isActive()) {
            throw new InactiveUserException(user.getId());
        }

        Event event = eventRepository
                .findByEventCode(TextNormalizer.code(request.eventCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Event", request.eventCode()));

        // Regla derivada: solo se emiten tickets para eventos PUBLISHED (BR-010 deja
        // SOLD_OUT fuera de este MVP, pero DRAFT/CANCELLED/FINISHED tampoco venden)
        if (!event.getStatus().isSellable()) {
            throw new EventNotSellableException(event.getEventCode(), event.getStatus());
        }

        // Regla derivada: si el evento exige edad minima y el usuario ya tiene perfil
        // (con fecha de nacimiento), se valida la edad. Sin perfil no se puede calcular,
        // así que esta etapa del MVP no bloquea la compra en ese caso.
        if (event.getMinimumAge() != null && event.getMinimumAge() > 0) {
            UserProfile profile = user.getProfile();
            if (profile != null) {
                int age = Period.between(profile.getBirthDate(), LocalDate.now(clock)).getYears();
                if (age < event.getMinimumAge()) {
                    throw new UnderAgeException(profile.getBirthDate(), event.getMinimumAge());
                }
            }
        }

        String ticketCode = TextNormalizer.code(request.ticketCode());

        // FR-TKT-002: ticketCode unico
        if (ticketRepository.existsByTicketCode(ticketCode)) {
            throw new DuplicateResourceException("Ticket", "ticketCode", ticketCode);
        }

        Ticket ticket = Ticket.create(
                ticketCode,
                request.type(),
                TicketStatus.RESERVED,
                request.price(),
                LocalDateTime.now(clock),
                user,
                event
        );

        return ticketMapper.toDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketDto pay(Long ticketId) {
        Ticket ticket = findTicket(ticketId);
        ticket.pay();
        return ticketMapper.toDto(ticket);
    }

    @Override
    @Transactional
    public TicketDto cancel(Long ticketId) {
        Ticket ticket = findTicket(ticketId);
        ticket.cancel();
        return ticketMapper.toDto(ticket);
    }

    @Override
    @Transactional
    public TicketDto markUsed(Long ticketId) {
        Ticket ticket = findTicket(ticketId);
        ticket.markUsed();
        return ticketMapper.toDto(ticket);
    }

    @Override
    public TicketDto findById(Long id) {
        return ticketMapper.toDto(findTicket(id));
    }

    @Override
    public TicketDto findByTicketCode(String ticketCode) {
        String normalized = TextNormalizer.code(ticketCode);

        return ticketRepository
                .findByTicketCode(normalized)
                .map(ticketMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", normalized));
    }

    @Override
    public List<TicketDto> findByUserEmail(String email) {
        return ticketRepository
                .findByUser_Email(TextNormalizer.email(email))
                .stream()
                .map(ticketMapper::toDto)
                .toList();
    }

    @Override
    public List<TicketDto> findByUserEmailAndStatus(String email, TicketStatus status) {
        return ticketRepository
                .findByUser_EmailAndStatus(TextNormalizer.email(email), status)
                .stream()
                .map(ticketMapper::toDto)
                .toList();
    }

    @Override
    public List<TicketDto> findPaidByEvent(String eventCode) {
        return ticketRepository
                .findByEvent_EventCodeAndStatus(TextNormalizer.code(eventCode), TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toDto)
                .toList();
    }

    @Override
    public long countPaidByEvent(String eventCode) {
        return ticketRepository.countByEventCodeAndStatus(TextNormalizer.code(eventCode), TicketStatus.PAID);
    }

    @Override
    public List<TicketDto> findByEventDateAfter(LocalDateTime after) {
        return ticketRepository
                .findByEventDateAfter(after)
                .stream()
                .map(ticketMapper::toDto)
                .toList();
    }

    private Ticket findTicket(Long id) {
        return ticketRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }
}
