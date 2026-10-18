package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** identifier = username o email. */
public record LoginRequest(

        @NotBlank(message = "Identifier is required") @Size(max = 150, message = "Identifier must be at most 150 characters") String identifier,

        @NotBlank(message = "Password is required") @Size(max = 200, message = "Password is too long") String password
) {

    /** Nunca imprimir la contraseña (SEC003). */
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + ", password=***]";
    }
}
