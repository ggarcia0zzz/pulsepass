package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Lee el encabezado Authorization: Bearer, valida el JWT y construye el contexto de seguridad
 * con un principal cargado desde la base de datos.
 *
 * Política: este filtro NUNCA rechaza por sí mismo. Si no hay Bearer, o es inválido, deja el
 * contexto vacío y continúa; la cadena decide si la ruta es pública (permitAll) o responde 401.
 * Un Bearer inválido jamás autentica. No crea cuentas ni persiste tokens.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_LENGTH = 4096;

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AccountStatusUserDetailsChecker statusChecker = new AccountStatusUserDetailsChecker();

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // No sobrescribir una autenticación ya establecida.
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
            chain.doFilter(request, response);
            return;
        }

        List<String> headers = request.getHeaders(HttpHeaders.AUTHORIZATION) == null
                ? List.of()
                : Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        if (headers.size() > 1) {
            // Doble encabezado sospechoso: no se autentica.
            log.warn("Multiple Authorization headers received; request left unauthenticated");
        } else if (headers.size() == 1) {
            String header = headers.get(0);
            if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                String token = header.substring(BEARER_PREFIX.length()).trim();
                authenticate(request, token);
            }
            // Otros esquemas (Basic, etc.) se ignoran: la API solo acepta Bearer.
        }

        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            if (token.length() > MAX_TOKEN_LENGTH) {
                throw new InvalidJwtException("Token too long");
            }
            Long userId = jwtService.extractUserId(token);
            SecurityPrincipal principal = userDetailsService.loadUserById(userId);
            statusChecker.check(principal); // inactivo/bloqueado/expirado -> excepción

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            // Sin detalles del token en el log (el JWT es un secreto).
            log.debug("Bearer token rejected: {}", ex.getClass().getSimpleName());
        }
    }
}
