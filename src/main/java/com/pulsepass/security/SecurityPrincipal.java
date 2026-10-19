package com.pulsepass.security;

import com.pulsepass.domain.enums.Role;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Identidad autenticada. Se construye SIEMPRE desde la base de datos (usuario persistido,
 * estado activo y roles vigentes), nunca desde claims del cliente.
 */
public final class SecurityPrincipal implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String username;
    private String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public SecurityPrincipal(Long id, String username, String passwordHash, boolean active,
                             Collection<Role> roles) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }

    public Long getId() {
        return id;
    }

    public boolean hasRole(Role role) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(role.authority()));
    }

    /** Nombres de rol sin prefijo (USER, STAFF, ADMIN). */
    public List<String> roleNames() {
        return authorities.stream()
                .map(a -> a.getAuthority().substring("ROLE_".length()))
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Un usuario desactivado pierde acceso en el siguiente request (SEC011). */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SecurityPrincipal other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SecurityPrincipal{id=" + id + ", username=" + username + ", roles=" + roleNames() + "}";
    }
}
