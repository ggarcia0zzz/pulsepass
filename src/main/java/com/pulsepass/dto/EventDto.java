package com.pulsepass.dto;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record EventDto(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        Integer minimumAge,
        String streamingUrl,
        String venueCode,
        String venueName,
        Set<String> artistStageNames
) {
}
