package com.pulsepass.dto;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        String ticketCode, TicketType type, TicketStatus status, BigDecimal price,
        LocalDateTime purchaseDate, String eventCode, String eventName, LocalDateTime eventDate, String userEmail) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getTicketCode(), t.getType(), t.getStatus(), t.getPrice(), t.getPurchaseDate(),
                t.getEvent().getEventCode(), t.getEvent().getName(), t.getEvent().getEventDate(), t.getUser().getEmail());
    }
}
