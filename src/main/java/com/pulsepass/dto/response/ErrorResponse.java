package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/** Contrato uniforme de error HTTP (PRD seccion 13). details solo se llena en errores de validacion. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
