package com.pulsepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** identifier = username o email. */
public record LoginRequest(@NotBlank @Size(max = 150) String identifier, @NotBlank @Size(max = 72) String password) {
}
