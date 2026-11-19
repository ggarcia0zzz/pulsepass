package com.pulsepass.security;

import com.pulsepass.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee unicamente "Authorization: Bearer ...". Un token ausente o invalido deja la peticion anonima:
 * en rutas protegidas el EntryPoint responde 401; en rutas publicas se ignora. El usuario y su rol
 * se recargan de la BD en cada peticion (cuenta desactivada o rol revocado pierden efecto de inmediato).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            try {
                Long userId = jwt.parseUserId(token);
                users.findById(userId).filter(u -> u.isActive()).ifPresent(u -> {
                    AuthenticatedUser principal = AuthenticatedUser.from(u);
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, authorities));
                });
            } catch (RuntimeException ignored) {
                // JwtException, IllegalArgumentException...: queda anonimo
            }
        }
        chain.doFilter(request, response);
    }
}
