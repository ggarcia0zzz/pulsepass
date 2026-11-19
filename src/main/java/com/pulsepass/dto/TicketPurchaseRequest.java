package com.pulsepass.dto;

import com.pulsepass.domain.enums.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketPurchaseRequest(
        @NotBlank @Email String userEmail,
        @NotBlank String eventCode,
        @NotNull TicketType type) {
}
