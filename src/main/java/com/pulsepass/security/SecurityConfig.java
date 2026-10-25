package com.pulsepass.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Declara filtro, autenticación y reglas de rutas. NO contiene negocio de compra/publicación.
 * Las reglas de propiedad (tickets, datos personales) y la visibilidad de borradores viven en
 * AuthorizationService / Services (seguridad a nivel de método), no aquí.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtService jwtService,
                                            CustomUserDetailsService userDetailsService,
                                            RestAuthenticationEntryPoint authEntryPoint,
                                            RestAccessDeniedHandler deniedHandler) throws Exception {

        // No es un @Bean: así Spring Boot no lo registra además como filtro de servlet (doble ejecución).
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                // Solo API Bearer: no hay cookies de autenticación enviadas automáticamente por el navegador.
                // Si algún día se usa autenticación por cookie, hay que revisar CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Rutas técnicas
                        .requestMatchers("/error").permitAll()

                        // Autenticación y registro (rol inicial exclusivamente USER, ver AuthService)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()

                        // Operaciones privilegiadas (rutas específicas ANTES de los patrones generales)
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")

                        // Lecturas públicas. GET /api/events/{eventCode} y /by-artist son públicos a nivel HTTP,
                        // pero EventServiceImpl oculta (404) todo evento no publicado a quien no sea STAFF/ADMIN.
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/published",
                                "/api/events/by-artist",
                                "/api/events/*",
                                "/api/artists/**",
                                "/api/venues/**").permitAll()

                        // Denegar por defecto (SEC001): todo lo demás exige autenticación.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /** Orígenes CORS parametrizados (NFR-SEC008). Lista vacía = ningún origen cruzado permitido. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${pulsepass.security.cors.allowed-origins:}") List<String> allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream().filter(o -> !o.isBlank()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", RequestIdFilter.HEADER));
        configuration.setExposedHeaders(List.of(RequestIdFilter.HEADER));
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
