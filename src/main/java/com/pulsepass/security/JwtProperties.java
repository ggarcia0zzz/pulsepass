package com.pulsepass.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración JWT (pulsepass.security.jwt.*). La clave llega SOLO desde el entorno
 * (PULSEPASS_JWT_SECRET_BASE64); JwtService falla al arrancar si falta o es débil.
 */
@Validated
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        @NotBlank @DefaultValue("pulsepass-api") String issuer,
        @NotBlank String secretBase64,
        @DefaultValue("PT15M") Duration accessTokenTtl
) {

    /** Evita que la clave aparezca en logs si alguien imprime el objeto. */
    @Override
    public String toString() {
        return "JwtProperties{issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + ", secretBase64=***}";
    }
}
