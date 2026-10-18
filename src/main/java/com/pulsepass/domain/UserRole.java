package com.pulsepass.domain;

import com.pulsepass.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Un rol asignado a un usuario. Un usuario puede tener varios (PK compuesta). */
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    protected UserRole() {
    }

    private UserRole(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public static UserRole of(Long userId, Role role) {
        return new UserRole(userId, role);
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }
}
