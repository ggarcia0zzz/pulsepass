package com.pulsepass.dto;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketDto(
        Long id,
        String ticketCode,
        TicketType type,
        TicketStatus status,
        BigDecimal price,
        LocalDateTime purchaseDate,
        String userEmail,
        String eventCode
) {
}
