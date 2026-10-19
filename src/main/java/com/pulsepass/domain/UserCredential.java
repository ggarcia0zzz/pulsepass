package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Credencial 1:1 con User. Vive en su propia tabla para que el hash no pueda
 * serializarse por accidente desde User/UserProfile ni desde MapStruct.
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    protected UserCredential() {
    }

    private UserCredential(User user, String passwordHash, LocalDateTime passwordChangedAt) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
    }

    public static UserCredential create(User user, String passwordHash, LocalDateTime now) {
        return new UserCredential(user, passwordHash, now);
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    /** Nunca incluir el hash en toString (evita filtrarlo a logs). */
    @Override
    public String toString() {
        return "UserCredential{userId=" + userId + "}";
    }
}
