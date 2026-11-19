package com.pulsepass.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** No tiene campo role: cualquier "role" enviado en el JSON se ignora. */
public record UserRegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "solo letras, numeros, punto, guion y guion bajo")
        String username,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 30) String phone,
        @Size(max = 100) String city,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe incluir letras y numeros")
        String password) {
}
