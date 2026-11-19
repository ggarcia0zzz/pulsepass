package com.pulsepass.dto;

import com.pulsepass.domain.enums.EventCategory;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "solo letras, numeros, guion y guion bajo")
        String eventCode,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull EventCategory category,
        @NotNull @Future LocalDateTime eventDate,
        @NotNull @Min(0) @Max(120) Integer minimumAge,
        @Size(max = 500) String streamingUrl,
        @NotBlank @Size(max = 50) String venueCode) {
}
