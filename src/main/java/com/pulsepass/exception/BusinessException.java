package com.pulsepass.exception;

/** Base de todos los errores de regla de negocio. */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
