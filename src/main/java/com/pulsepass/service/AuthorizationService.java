package com.pulsepass.service;

import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.shared.TextNormalizer;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Compara el principal autenticado con el propietario del recurso y comprueba permisos.
 * Se usa desde SpEL en @PreAuthorize ("@authorizationService.canXxx(...)") y desde los Services.
 *
 * Reglas:
 *  - La propiedad se compara por ID persistente (nunca por un string de email del cliente).
 *  - Política única anti-enumeración: para un USER, un ticket/usuario ajeno y uno inexistente
 *    responden igual (403). STAFF/ADMIN reciben 404 si el recurso no existe.
 *  - STAFF es global en los endpoints de operación (el dominio aún no modela asignación a eventos):
 *    limitación del MVP que debe restringirse en producción.
 */
@Service("authorizationService")
public class AuthorizationService {

    /** Estados visibles públicamente. Para exponer también SOLD_OUT basta añadirlo aquí. */
    private static final Set<EventStatus> PUBLIC_EVENT_STATUSES = EnumSet.of(EventStatus.PUBLISHED);

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // ---------- Identidad actual ----------

    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof SecurityPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }

    // ---------- Tickets ----------

    /** POST /api/tickets: solo USER, y el userEmail del DTO debe pertenecer al principal. */
    public boolean canPurchaseFor(String userEmail) {
        return currentPrincipal()
                .filter(p -> p.hasRole(Role.USER))
                .map(p -> isSameUserByEmail(p, userEmail))
                .orElse(false);
    }

    /** GET /api/tickets/{ticketCode}: propietario, o STAFF/ADMIN. */
    public boolean canViewTicket(String ticketCode) {
        return currentPrincipal()
                .map(p -> isStaffOrAdmin(p) || isTicketOwner(p, ticketCode))
                .orElse(false);
    }

    /** PATCH /api/tickets/{ticketCode}/cancel: propietario, o ADMIN. Se evalúa ANTES de mutar estado. */
    public boolean canCancelTicket(String ticketCode) {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.ADMIN) || isTicketOwner(p, ticketCode))
                .orElse(false);
    }

    /** GET /api/tickets/by-user?email=: propietario, o ADMIN. */
    public boolean canViewTicketsOfEmail(String email) {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.ADMIN) || isSameUserByEmail(p, email))
                .orElse(false);
    }

    // ---------- Usuarios ----------

    /** GET /api/users/by-email?email=: el propio usuario, o ADMIN. */
    public boolean canViewUserByEmail(String email) {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.ADMIN) || isSameUserByEmail(p, email))
                .orElse(false);
    }

    /** GET /api/users/by-username?username=: el propio usuario, o ADMIN. */
    public boolean canViewUserByUsername(String username) {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.ADMIN) || isSameUserByUsername(p, username))
                .orElse(false);
    }

    // ---------- Eventos ----------

    /** STAFF/ADMIN pueden ver eventos en cualquier estado (borradores incluidos). */
    public boolean canViewUnpublishedEvents() {
        return currentPrincipal().map(this::isStaffOrAdmin).orElse(false);
    }

    /** Público para eventos publicados; el resto solo para STAFF/ADMIN. */
    public boolean canViewEvent(EventStatus status) {
        return PUBLIC_EVENT_STATUSES.contains(status) || canViewUnpublishedEvents();
    }

    // ---------- helpers ----------

    private boolean isStaffOrAdmin(SecurityPrincipal principal) {
        return principal.hasRole(Role.STAFF) || principal.hasRole(Role.ADMIN);
    }

    private boolean isTicketOwner(SecurityPrincipal principal, String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            return false;
        }
        return ticketRepository.findOwnerIdByTicketCode(TextNormalizer.code(ticketCode))
                .filter(ownerId -> ownerId.equals(principal.getId()))
                .isPresent();
    }

    private boolean isSameUserByEmail(SecurityPrincipal principal, String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(TextNormalizer.email(email))
                .filter(user -> user.getId().equals(principal.getId()))
                .isPresent();
    }

    private boolean isSameUserByUsername(SecurityPrincipal principal, String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return userRepository.findByUsername(TextNormalizer.text(username))
                .filter(user -> user.getId().equals(principal.getId()))
                .isPresent();
    }
}
