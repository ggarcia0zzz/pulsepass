package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.EventCategory;
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

        @NotBlank @Size(max = 50) String eventCode,

        @NotBlank @Size(max = 200) String name,

        @Size(max = 1000) String description,

        @NotNull EventCategory category,

        @NotNull LocalDateTime eventDate,

        Integer minimumAge,

        @NotBlank String venueCode
) {
}
