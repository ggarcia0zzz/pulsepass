package com.pulsepass.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignArtistDto(@NotBlank String stageName) {
}
