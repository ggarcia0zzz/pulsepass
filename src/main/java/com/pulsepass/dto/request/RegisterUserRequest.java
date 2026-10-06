package com.pulsepass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** birthDate no puede ser futura (BR-USER-005): se valida en UserServiceImpl. */
public record RegisterUserRequest(

        @NotBlank @Size(max = 50) String username,

        @NotBlank @Email @Size(max = 150) String email,

        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotBlank @Size(max = 30) String phone,

        @Size(max = 100) String city,

        @NotNull LocalDate birthDate
) {
}
