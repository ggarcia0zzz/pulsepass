package com.pulsepass.controller;

import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz HTTP de la SecurityConfig REAL (rol por ruta). Las denegaciones ocurren ANTES de llegar al Service,
 * por eso no dependen de cómo se comporten los mocks. La propiedad (IDOR) se prueba en SecurityMatrixIT.
 */
@WebMvcTest({EventController.class, ArtistController.class, VenueController.class,
        TicketController.class, UserController.class, AuthController.class})
class SecurityRulesTest extends SecuredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private ArtistService artistService;
    @MockitoBean
    private VenueService venueService;
    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthService authService;

    // ---------- Rutas públicas (sin token) ----------

    @ParameterizedTest(name = "[{index}] anónimo {0} {1} -> público")
    @DisplayName("Rutas públicas accesibles sin token (T-SEC-001)")
    @CsvSource({
            "GET, /api/venues/VEN-SMR-01",
            "GET, /api/venues/active",
            "GET, /api/events/published",
            "GET, /api/events/CMF-2026",
            "GET, /api/events/by-artist?stageName=Solar%20Beat",
            "GET, /api/artists/1",
            "GET, /api/artists/by-stage-name?stageName=Solar%20Beat",
            "GET, /api/artists/active"
    })
    void publicRoutes_doNotRequireAuthentication(String method, String path) throws Exception {
        int status = mockMvc.perform(request(HttpMethod.valueOf(method), path).with(anonymous()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    // ---------- Rutas protegidas: anónimo -> 401 (T-SEC-002) ----------

    @ParameterizedTest(name = "[{index}] anónimo {0} {1} -> 401")
    @DisplayName("Anónimo en ruta protegida -> 401 con ErrorResponse")
    @CsvSource({
            "POST,  /api/events",
            "PATCH, /api/events/CMF-2026/publish",
            "POST,  /api/events/CMF-2026/artists/1",
            "GET,   /api/events/CMF-2026/tickets/paid",
            "POST,  /api/tickets",
            "GET,   /api/tickets/TCK-1",
            "GET,   /api/tickets/by-user?email=a@b.com",
            "PATCH, /api/tickets/TCK-1/cancel",
            "PATCH, /api/tickets/TCK-1/use",
            "GET,   /api/users/by-email?email=a@b.com",
            "GET,   /api/users/by-username?username=andrea",
            "GET,   /api/auth/me"
    })
    void protectedRoutes_anonymous_returns401(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verifyNoInteractions(eventService, ticketService, userService);
    }

    // ---------- USER en rutas de ADMIN/STAFF -> 403 (T-SEC-003, T-SEC-012) ----------

    @ParameterizedTest(name = "[{index}] USER {0} {1} -> 403")
    @DisplayName("USER en rutas privilegiadas -> 403 sin invocar el Service")
    @CsvSource({
            "POST,  /api/events",
            "PATCH, /api/events/CMF-2026/publish",
            "POST,  /api/events/CMF-2026/artists/1",
            "GET,   /api/events/CMF-2026/tickets/paid",
            "PATCH, /api/tickets/TCK-1/use"
    })
    void privilegedRoutes_user_returns403(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(user("andrea").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"));

        verifyNoInteractions(eventService, ticketService);
    }

    // ---------- STAFF: sí opera tickets, no administra eventos ----------

    @ParameterizedTest(name = "[{index}] STAFF {0} {1} -> 403")
    @DisplayName("STAFF no puede crear/publicar eventos ni asociar artistas")
    @CsvSource({
            "POST,  /api/events",
            "PATCH, /api/events/CMF-2026/publish",
            "POST,  /api/events/CMF-2026/artists/1"
    })
    void adminRoutes_staff_returns403(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(user("laura").roles("STAFF")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(eventService);
    }

    @ParameterizedTest(name = "[{index}] STAFF {0} {1} -> permitido")
    @DisplayName("STAFF puede usar tickets y listar pagados (T-SEC-013)")
    @CsvSource({
            "PATCH, /api/tickets/TCK-1/use",
            "GET,   /api/events/CMF-2026/tickets/paid"
    })
    void staffRoutes_staff_isAllowed(String method, String path) throws Exception {
        int status = mockMvc.perform(request(HttpMethod.valueOf(method), path).with(user("laura").roles("STAFF")))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    // ---------- ADMIN ----------

    @ParameterizedTest(name = "[{index}] ADMIN {0} {1} -> permitido")
    @DisplayName("ADMIN accede a las rutas de administración (T-SEC-004, T-SEC-014)")
    @CsvSource({
            "POST,  /api/events",
            "PATCH, /api/events/CMF-2026/publish",
            "POST,  /api/events/CMF-2026/artists/1",
            "PATCH, /api/tickets/TCK-1/use",
            "GET,   /api/events/CMF-2026/tickets/paid"
    })
    void adminRoutes_admin_isAllowed(String method, String path) throws Exception {
        int status = mockMvc.perform(request(HttpMethod.valueOf(method), path).with(user("admin").roles("ADMIN")))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    // ---------- Denegar por defecto ----------

    @ParameterizedTest(name = "[{index}] anónimo {0} {1} -> 401 (denegar por defecto)")
    @DisplayName("Ruta no declarada -> exige autenticación (SEC001)")
    @CsvSource({
            "GET,    /api/ruta-no-declarada",
            "DELETE, /api/events/CMF-2026",
            "POST,   /actuator/env"
    })
    void undeclaredRoutes_areDeniedByDefault(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}
