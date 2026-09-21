package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // FR-TKT-002 / AC-005: recuperar un ticket por su código
    Optional<Ticket> findByTicketCode(String ticketCode);

    // FR-TKT-006: tickets de un usuario por email (Ticket -> User -> email)
    List<Ticket> findByUser_Email(String email);

    // FR-TKT-006: tickets de un usuario por email y estado
    List<Ticket> findByUser_EmailAndStatus(String email, TicketStatus status);

    // FR-TKT-007: tickets de un evento por eventCode y estado
    // (se usa con TicketStatus.PAID para los tickets pagados).
    // Query Method porque el filtro es simple: dos condiciones de igualdad.
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    // FR-TKT-008 / AC-008: conteo de tickets de un evento por estado
    // (se usa con TicketStatus.PAID para el conteo de ventas)
    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = :status
            """)
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode,
                                   @Param("status") TicketStatus status);

    // FR-SRC-004: tickets de eventos posteriores a una fecha, en orden cronológico
    @Query("""
            SELECT t
            FROM Ticket t
            JOIN t.event e
            WHERE e.eventDate > :after
            ORDER BY e.eventDate ASC
            """)
    List<Ticket> findByEventDateAfter(@Param("after") LocalDateTime after);
}