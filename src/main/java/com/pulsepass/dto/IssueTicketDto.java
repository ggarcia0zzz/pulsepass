package com.pulsepass.dto;

import com.pulsepass.domain.enums.TicketType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IssueTicketDto(

        @NotBlank @Size(max = 50) String ticketCode,

        @NotNull TicketType type,

        // NUMERIC(10,2) en la BD: hasta 8 enteros y 2 decimales
        @NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal price,

        @NotBlank String userEmail,

        @NotBlank String eventCode
) {
}
