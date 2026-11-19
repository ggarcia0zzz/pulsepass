package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.UserRole;

public record AuthenticatedUser(Long id, String username, String email, UserRole role) {

    public static AuthenticatedUser from(User u) {
        return new AuthenticatedUser(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }

    public boolean isAdmin() { return role == UserRole.ADMIN; }

    /** STAFF o ADMIN (OPS en la matriz del PRD). */
    public boolean isOps() { return role == UserRole.ADMIN || role == UserRole.STAFF; }

    public boolean owns(String email) { return this.email.equalsIgnoreCase(email == null ? "" : email.trim()); }
}
