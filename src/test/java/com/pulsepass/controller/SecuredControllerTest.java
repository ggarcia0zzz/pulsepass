package com.pulsepass.controller;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Base de los @WebMvcTest: carga la SecurityConfig REAL (filtros activos, reglas de rutas, 401/403 JSON).
 * NO se usa addFilters = false: el objetivo es probar seguridad, no esquivarla.
 *
 * Por defecto cada prueba corre con una identidad con los tres roles, para que las pruebas de mapeo,
 * validación y errores de la Fase 3 sigan siendo válidas. Las pruebas de rol/propiedad usan identidades
 * explícitas (ver SecurityRulesTest y SecurityMatrixIT).
 */
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@WithMockUser(username = "tester", roles = {"USER", "STAFF", "ADMIN"})
abstract class SecuredControllerTest {

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    protected AuthorizationService authorizationService;

    /**
     * Los Services mockeados con @PreAuthorize("@authorizationService...") pueden quedar envueltos por la
     * seguridad de métodos. Aquí se permiten las comprobaciones de PROPIEDAD para que estas pruebas se
     * centren en el mapeo HTTP; la propiedad real se prueba con la base de datos en SecurityMatrixIT.
     */
    @BeforeEach
    void allowOwnershipChecks() {
        when(authorizationService.canPurchaseFor(any())).thenReturn(true);
        when(authorizationService.canViewTicket(any())).thenReturn(true);
        when(authorizationService.canCancelTicket(any())).thenReturn(true);
        when(authorizationService.canViewTicketsOfEmail(any())).thenReturn(true);
        when(authorizationService.canViewUserByEmail(any())).thenReturn(true);
        when(authorizationService.canViewUserByUsername(any())).thenReturn(true);
    }
}
