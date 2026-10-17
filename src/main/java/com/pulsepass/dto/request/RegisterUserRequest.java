package com.pulsepass.dto.request;

import com.pulsepass.dto.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * birthDate no puede ser futura (BR-USER-005): se valida en UserServiceImpl.
 *
 * Fase 4: se añade password (mínimo académico 12 caracteres; máximo 72 BYTES por el límite de BCrypt).
 * No existe campo de rol: el registro público siempre crea ROLE_USER.
 */
public record RegisterUserRequest(

        @NotBlank(message = "Username is required") @Size(max = 50, message = "Username must be at most 50 characters") String username,

        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 150, message = "Email must be at most 150 characters") String email,

        @NotBlank(message = "First name is required") @Size(max = 100, message = "First name must be at most 100 characters") String firstName,

        @NotBlank(message = "Last name is required") @Size(max = 100, message = "Last name must be at most 100 characters") String lastName,

        @NotBlank(message = "Phone is required") @Size(max = 30, message = "Phone must be at most 30 characters") String phone,

        @Size(max = 100, message = "City must be at most 100 characters") String city,

        @NotNull(message = "Birth date is required") LocalDate birthDate,

        @NotBlank(message = "Password is required")
        @Size(min = 12, message = "Password must be at least 12 characters")
        @MaxBytes(value = 72, message = "Password must be at most 72 bytes") String password
) {

    /** Nunca imprimir la contraseña (SEC003). */
    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}
