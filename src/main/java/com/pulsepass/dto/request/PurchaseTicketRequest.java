package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** El precio NO viaja en el request: lo calcula el sistema (TicketPriceCalculator). */
public record PurchaseTicketRequest(

        @NotBlank String userEmail,

        @NotBlank String eventCode,

        @NotNull TicketType type
) {
}
