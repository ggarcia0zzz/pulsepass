package com.pulsepass.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/** 401/403 generados por el filtro de seguridad, con el mismo formato ErrorResponse que los controladores. */
public final class SecurityErrorHandlers {
    private SecurityErrorHandlers() {}

    public static final AuthenticationEntryPoint ENTRY_POINT =
            (request, response, ex) -> write(response, HttpStatus.UNAUTHORIZED, "Autenticacion requerida o token invalido");

    public static final AccessDeniedHandler ACCESS_DENIED =
            (request, response, ex) -> write(response, HttpStatus.FORBIDDEN, "No tienes permisos para esta operacion");

    private static void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\",\"details\":{}}");
    }
}
