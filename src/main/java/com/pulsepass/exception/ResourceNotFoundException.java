package com.pulsepass.exception;

/** El recurso solicitado no existe (ej. "Event not found: CMF-2026"). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s not found: %s".formatted(resource, identifier));
    }
}
