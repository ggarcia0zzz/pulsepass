package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.dto.TicketPurchaseRequest;
import com.pulsepass.dto.TicketResponse;
import com.pulsepass.exception.ConflictException;
import com.pulsepass.exception.ForbiddenException;
import com.pulsepass.exception.NotFoundException;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TicketService {
    private static final List<TicketStatus> OCCUPY_SEAT = List.of(TicketStatus.RESERVED, TicketStatus.PAID, TicketStatus.USED);

    private final TicketRepository tickets;
    private final EventRepository events;
    private final UserRepository users;

    public TicketService(TicketRepository tickets, EventRepository events, UserRepository users) {
        this.tickets = tickets;
        this.events = events;
        this.users = users;
    }

    public TicketResponse purchase(TicketPurchaseRequest req, AuthenticatedUser actor) {
        // El correo de la compra debe ser el del usuario autenticado, sin excepciones (ni ADMIN).
        if (!actor.owns(req.userEmail())) throw new ForbiddenException("No puedes comprar tickets a nombre de otro usuario");
        User user = users.findById(actor.id()).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!user.isActive()) throw new ForbiddenException("Cuenta inactiva");

        Event event = events.findByEventCodeForUpdate(req.eventCode().trim())
                .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        if (event.getStatus() != EventStatus.PUBLISHED) throw new ConflictException("El evento no esta disponible para la venta");
        if (!event.getEventDate().isAfter(LocalDateTime.now())) throw new ConflictException("El evento ya paso");

        if (event.getMinimumAge() > 0) {
            if (user.getProfile() == null) throw new ConflictException("No se puede verificar la edad minima");
            int age = Period.between(user.getProfile().getBirthDate(), LocalDate.now()).getYears();
            if (age < event.getMinimumAge()) throw new ConflictException("No cumples la edad minima del evento");
        }
        long sold = tickets.countByEvent_EventCodeAndStatusIn(event.getEventCode(), OCCUPY_SEAT);
        if (sold >= event.getVenue().getCapacity()) throw new ConflictException("Aforo agotado");

        Ticket ticket = Ticket.create("TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(),
                req.type(), TicketStatus.PAID, TicketPricing.priceFor(req.type()), LocalDateTime.now(), user, event);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    /** Propietario o STAFF/ADMIN. Ticket ajeno -> 403. */
    @Transactional(readOnly = true)
    public TicketResponse getByCode(String ticketCode, AuthenticatedUser actor) {
        Ticket ticket = find(ticketCode);
        if (!ticket.getUser().getId().equals(actor.id()) && !actor.isOps()) throw new ForbiddenException("Acceso denegado");
        return TicketResponse.from(ticket);
    }

    /** Propietario o ADMIN. */
    @Transactional(readOnly = true)
    public List<TicketResponse> listByUser(String email, AuthenticatedUser actor) {
        if (!actor.owns(email) && !actor.isAdmin()) throw new ForbiddenException("Acceso denegado");
        return tickets.findByUser_Email(email.trim().toLowerCase()).stream().map(TicketResponse::from).toList();
    }

    /** STAFF/ADMIN (lo exige SecurityConfig). */
    @Transactional(readOnly = true)
    public List<TicketResponse> listPaidByEvent(String eventCode) {
        if (events.findByEventCode(eventCode).isEmpty()) throw new NotFoundException("Evento no encontrado");
        return tickets.findByEvent_EventCodeAndStatus(eventCode, TicketStatus.PAID).stream().map(TicketResponse::from).toList();
    }

    /** Propietario o ADMIN. Solo RESERVED/PAID; repetir o cancelar un USED -> 409 sin mutar. */
    public TicketResponse cancel(String ticketCode, AuthenticatedUser actor) {
        Ticket ticket = find(ticketCode);
        if (!ticket.getUser().getId().equals(actor.id()) && !actor.isAdmin()) throw new ForbiddenException("Acceso denegado");
        if (ticket.getStatus() != TicketStatus.PAID && ticket.getStatus() != TicketStatus.RESERVED) {
            throw new ConflictException("El ticket no se puede cancelar en estado " + ticket.getStatus());
        }
        ticket.cancel();
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    /** STAFF/ADMIN (lo exige SecurityConfig). Solo PAID -> USED. */
    public TicketResponse use(String ticketCode) {
        Ticket ticket = find(ticketCode);
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new ConflictException("El ticket no se puede usar en estado " + ticket.getStatus());
        }
        ticket.markUsed();
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    private Ticket find(String ticketCode) {
        return tickets.findByTicketCode(ticketCode).orElseThrow(() -> new NotFoundException("Ticket no encontrado"));
    }
}
