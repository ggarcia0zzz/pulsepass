package com.pulsepass.exception;

/** Regla de negocio o estado incompatible -> 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
