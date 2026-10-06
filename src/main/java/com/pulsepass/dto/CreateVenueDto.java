package com.pulsepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateVenueDto(@NotBlank @Size(max = 50) String code,

                             @NotBlank @Size(max = 150) String name,

                             @NotBlank @Size(max = 100) String city,

                             @NotBlank @Size(max = 255) String address,

                             @Positive Integer capacity) {
}
