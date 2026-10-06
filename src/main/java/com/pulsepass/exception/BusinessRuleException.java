package com.pulsepass.exception;

/**
 * El recurso existe, pero la operacion solicitada no es valida segun las reglas de negocio
 * (ej. "User does not meet minimum age", transiciones de estado invalidas, capacidad agotada).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
