package com.pulsepass.domain.enums;

/** Roles de la aplicación. En Spring Security se manejan como authorities con prefijo ROLE_. */
public enum Role {
    USER,
    STAFF,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
