package com.pulsepass.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserDto(

        @NotBlank @Size(max = 50) String username,

        @NotBlank @Email @Size(max = 150) String email
) {
}
