package com.pulsepass.exception;

/** Recurso ajeno o privilegio insuficiente detectado en el Service -> 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
