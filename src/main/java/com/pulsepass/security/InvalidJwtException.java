package com.pulsepass.security;

import org.springframework.security.core.AuthenticationException;

/** JWT vencido, manipulado, de otro emisor, malformado o con algoritmo inesperado. */
public class InvalidJwtException extends AuthenticationException {

    public InvalidJwtException(String message) {
        super(message);
    }

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
