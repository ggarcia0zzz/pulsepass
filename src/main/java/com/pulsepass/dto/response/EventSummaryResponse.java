package com.pulsepass.dto.response;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

import java.time.LocalDateTime;

/** Vista liviana para listados (cartelera, busqueda por artista). */
public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        String venueCode,
        String venueName
) {
}
