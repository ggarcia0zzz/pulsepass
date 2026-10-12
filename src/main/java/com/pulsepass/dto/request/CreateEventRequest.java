package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Solo validacion estructural (campos obligatorios y tamanos). Las reglas de negocio
 * (fecha futura, edad minima >= 0, venue activo...) viven en EventServiceImpl y lanzan
 * BusinessRuleException; por eso aqui no hay @Future ni @Min.
 * El estado inicial NO viene en el request: todo evento nuevo inicia en DRAFT (BR-EVENT-005).
 * minimumAge null se interpreta como 0 (sin restriccion de edad).
 */
public record CreateEventRequest(

        @NotBlank(message = "Event code is required") @Size(max = 50, message = "Event code must be at most 50 characters") String eventCode,

        @NotBlank(message = "Name is required") @Size(max = 200, message = "Name must be at most 200 characters") String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters") String description,

        @NotNull(message = "Category is required") EventCategory category,

        @NotNull(message = "Event date is required") LocalDateTime eventDate,

        @Min(value = 0, message = "Minimum age cannot be negative") Integer minimumAge,

        @NotBlank(message = "Venue code is required") String venueCode
) {
}
