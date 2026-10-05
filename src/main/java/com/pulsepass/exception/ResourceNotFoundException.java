package com.pulsepass.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s not found: %s".formatted(resource, identifier));
    }
}
