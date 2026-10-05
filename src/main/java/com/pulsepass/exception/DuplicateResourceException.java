package com.pulsepass.exception;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resource, String field, String value) {
        super("%s already exists with %s: %s".formatted(resource, field, value));
    }
}
