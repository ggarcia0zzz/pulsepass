package com.pulsepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterArtistDto(

        @NotBlank @Size(max = 150) String stageName,

        @NotBlank @Size(max = 100) String genre,

        @NotBlank @Size(max = 100) String country
) {
}
