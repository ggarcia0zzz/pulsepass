package com.pulsepass.exception;

/** Conflicto de unicidad (ej. "User already exists with username: andrea"). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, String value) {
        super("%s already exists with %s: %s".formatted(resource, field, value));
    }
}
