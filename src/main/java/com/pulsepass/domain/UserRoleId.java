package com.pulsepass.domain;

import com.pulsepass.domain.enums.Role;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta (user_id, role) de la tabla user_roles. */
public class UserRoleId implements Serializable {

    private Long userId;
    private Role role;

    protected UserRoleId() {
    }

    public UserRoleId(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRoleId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }
}
