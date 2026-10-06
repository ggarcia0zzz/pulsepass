package com.pulsepass.dto;

import com.pulsepass.domain.enums.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateEventDto(

        @NotBlank @Size(max = 50) String eventCode,

        @NotBlank @Size(max = 200) String name,

        @Size(max = 1000) String description,

        @NotNull EventCategory category,

        @NotNull @Future LocalDateTime eventDate,

        @NotNull @Min(0) Integer minimumAge,

        @NotBlank String venueCode
) {
}
