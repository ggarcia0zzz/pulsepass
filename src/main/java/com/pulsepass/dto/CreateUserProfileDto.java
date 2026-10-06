package com.pulsepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateUserProfileDto(

        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotBlank @Size(max = 30) String phone,

        @Size(max = 100) String city,

        @NotNull @Past LocalDate birthDate
) {
}
