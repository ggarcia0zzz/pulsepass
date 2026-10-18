package com.pulsepass.integration;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtProperties;
import com.pulsepass.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad de extremo a extremo contra PostgreSQL real y la cadena de filtros REAL:
 * login, JWT, roles, propiedad (anti-IDOR), borradores, usuarios inactivos y regresión funcional.
 * Todos los secretos y contraseñas son ficticios.
 */
@AutoConfigureMockMvc
class SecurityMatrixIT extends IntegrationTestBase {

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserCredentialRepository credentialRepository;
    @Autowired
    private UserRoleRepository roleRepository;

    private Venue venue;
    private Event published;
    private Event draft;
    private User andrea;
    private User carlos;
    private User laura;
    private User admin;
    private Ticket andreaTicket;
    private Ticket carlosTicket;

    @BeforeEach
    void seed() {
        venue = saveVenue("VEN-SMR-01", "Santa Marta");
        published = event("CMF-2026", true);
        draft = event("DRAFT-2026", false);

        andrea = account("andrea", "andrea@email.com", Role.USER);
        carlos = account("carlos", "carlos@email.com", Role.USER);
        laura = account("laura", "laura@email.com", Role.STAFF);
        admin = account("admin", "admin@email.com", Role.ADMIN);

        andreaTicket = saveTicket("TCK-ANDREA", TicketType.GENERAL, TicketStatus.PAID, "100000.00", andrea, published);
        carlosTicket = saveTicket("TCK-CARLOS", TicketType.GENERAL, TicketStatus.PAID, "100000.00", carlos, published);
    }

    // ---------- helpers ----------

    private Event event(String code, boolean publish) {
        Event event = Event.create(code, "Evento " + code, "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(30), 0, venue);
        if (publish) {
            event.publish();
        }
        return eventRepository.saveAndFlush(event);
    }

    private User account(String username, String email, Role role) {
        User user = saveUser(username, email);
        credentialRepository.saveAndFlush(
                UserCredential.create(user, passwordEncoder.encode(PASSWORD), LocalDateTime.now()));
        roleRepository.saveAndFlush(UserRole.of(user.getId(), role));
        return user;
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getId());
    }

    private static String purchaseBody(String email) {
        return """
                {"userEmail": "%s", "eventCode": "CMF-2026", "type": "GENERAL"}
                """.formatted(email);
    }

    private static String registerBody(String username, String email, String extra) {
        return """
                {"username": "%s", "email": "%s", "firstName": "Nuevo", "lastName": "Usuario",
                 "phone": "3001234567", "city": "Santa Marta", "birthDate": "1995-05-10",
                 "password": "%s"%s}
                """.formatted(username, email, PASSWORD, extra);
    }

    // ---------- Registro y login (UC-SEC-01 / 02) ----------

    @Test
    @DisplayName("UC-SEC-01 registro: 201, sin hash en la respuesta, credencial BCrypt y SOLO ROLE_USER")
    void register_createsUserCredentialAndRoleUser_withoutExposingSecrets() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("nuevo", "nuevo@email.com", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD).doesNotContain("$2a$");

        User created = userRepository.findByUsername("nuevo").orElseThrow();
        UserCredential credential = credentialRepository.findById(created.getId()).orElseThrow();
        assertThat(credential.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(roleRepository.findByUserId(created.getId())).extracting(UserRole::getRole).containsExactly(Role.USER);
    }

    @Test
    @DisplayName("NFR-SEC009 el registro público no permite elegir roles (campo 'roles' ignorado)")
    void register_cannotSmuggleRoles() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("intruso", "intruso@email.com", ", \"roles\": [\"ADMIN\"], \"role\": \"ADMIN\"")))
                .andExpect(status().isCreated());

        User created = userRepository.findByUsername("intruso").orElseThrow();
        assertThat(roleRepository.findByUserId(created.getId())).extracting(UserRole::getRole).containsExactly(Role.USER);
    }

    @Test
    void register_duplicateUsername_returns409_andShortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("andrea", "otro@email.com", "")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("corto", "corto@email.com", "").replace(PASSWORD, "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    @DisplayName("T-SEC-005 / UC-SEC-02 login correcto: JWT con sub = ID persistente y expiresIn = 900")
    void login_valid_returnsBearerToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"andrea\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        assertThat(jwtService.extractUserId(token)).isEqualTo(andrea.getId());
    }

    @Test
    @DisplayName("Login por correo también funciona")
    void login_byEmail_works() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"Andrea@Email.com\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-006 contraseña incorrecta y usuario inexistente dan EXACTAMENTE el mismo 401")
    void login_wrongPasswordAndUnknownUser_areIndistinguishable() throws Exception {
        String wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"andrea\", \"password\": \"incorrecta-incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andReturn().getResponse().getContentAsString();

        String unknownUser = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"fantasma\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andReturn().getResponse().getContentAsString();

        assertThat(strip(wrongPassword)).isEqualTo(strip(unknownUser));
    }

    private static String strip(String json) {
        return json.replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }

    @Test
    @DisplayName("Un usuario legado sin credencial no puede iniciar sesión")
    void login_legacyUserWithoutCredential_isRejected() throws Exception {
        saveUser("legado", "legado@email.com");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"legado\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FR-SECAUTH-010 /me devuelve información mínima; FR-SECAUTH-007 el token NO se acepta por query string")
    void me_works_withBearer_andRejectsTokenInQueryString() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(laura)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.username").value("laura"))
                .andExpect(jsonPath("$.roles[0]").value("STAFF"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/auth/me").param("access_token", jwtService.generateToken(laura.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ---------- JWT inválido (UC-SEC-09) ----------

    @Test
    @DisplayName("T-SEC-007 token expirado -> 401 con ErrorResponse")
    void expiredToken_returns401() throws Exception {
        JwtService anHourAgo = new JwtService(jwtProperties, Clock.fixed(Instant.now().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC));
        String expired = anHourAgo.generateToken(andrea.getId());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("T-SEC-008 token con la firma alterada -> 401")
    void tamperedToken_returns401() throws Exception {
        String token = jwtService.generateToken(andrea.getId());
        int index = token.length() - 10;
        String tampered = token.substring(0, index) + (token.charAt(index) == 'A' ? 'B' : 'A') + token.substring(index + 1);

        mockMvc.perform(get("/api/tickets/{code}", "TCK-ANDREA").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T-SEC-008b emisor distinto y basura -> 401 (nunca 500)")
    void foreignIssuerAndGarbageTokens_return401() throws Exception {
        JwtProperties other = new JwtProperties("otro-emisor", jwtProperties.secretBase64(), jwtProperties.accessTokenTtl());
        String foreign = new JwtService(other, Clock.systemUTC()).generateToken(andrea.getId());

        for (String token : new String[]{foreign, "esto-no-es-un-jwt", "a.b.c", "x".repeat(5000)}) {
            mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Un Bearer inválido en ruta pública no concede nada y no rompe la ruta pública")
    void invalidBearer_onPublicRoute_staysAnonymous() throws Exception {
        mockMvc.perform(get("/api/events/published").header("Authorization", "Bearer basura"))
                .andExpect(status().isOk());
    }

    // ---------- Rutas públicas y borradores (UC-SEC-03, T-SEC-001, T-SEC-017) ----------

    @Test
    @DisplayName("T-SEC-001 anónimo ve eventos publicados y NO los borradores")
    void publishedEvents_arePublic_draftsAreNot() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventCode == 'CMF-2026')]").exists())
                .andExpect(jsonPath("$[?(@.eventCode == 'DRAFT-2026')]").isEmpty());
    }

    @Test
    @DisplayName("T-SEC-017 borrador por código: anónimo y USER reciben 404; STAFF y ADMIN lo ven")
    void draftByCode_hiddenFromPublicAndUsers_visibleToStaffAndAdmin() throws Exception {
        mockMvc.perform(get("/api/events/{code}", "DRAFT-2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: DRAFT-2026"));
        mockMvc.perform(get("/api/events/{code}", "DRAFT-2026").header("Authorization", bearer(andrea)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/events/{code}", "DRAFT-2026").header("Authorization", bearer(laura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(get("/api/events/{code}", "DRAFT-2026").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        // el publicado sí es público
        mockMvc.perform(get("/api/events/{code}", "CMF-2026")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-017b eventos por artista: el borrador solo lo ve STAFF/ADMIN")
    void eventsByArtist_hideDraftsFromPublic() throws Exception {
        draft.addArtist(artist("Solar Beat"));
        eventRepository.saveAndFlush(draft);

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat").header("Authorization", bearer(laura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void venuesAndArtists_arePublic() throws Exception {
        mockMvc.perform(get("/api/venues/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat")).andExpect(status().isOk());
    }

    // ---------- Eventos: roles (T-SEC-002/003/004/014/018, UC-SEC-08) ----------

    private static final String NEW_EVENT = """
            {"eventCode": "NEW-2026", "name": "Nuevo", "description": "d", "category": "MUSIC",
             "eventDate": "%s", "minimumAge": 0, "venueCode": "VEN-SMR-01"}
            """.formatted(LocalDateTime.now().plusDays(60).withNano(0));

    @Test
    void createEvent_anonymous401_user403_admin201() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(NEW_EVENT))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(NEW_EVENT)
                        .header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        assertThat(eventRepository.findByEventCode("NEW-2026")).isEmpty();

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(NEW_EVENT)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("T-SEC-018 petición inválida pero autorizada -> 400 de Bean Validation")
    void invalidButAuthorized_returns400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("UC-SEC-08 USER no publica (403, el evento sigue en DRAFT); ADMIN sí (200)")
    void publish_userForbidden_adminAllowed() throws Exception {
        mockMvc.perform(patch("/api/events/{code}/publish", "DRAFT-2026").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        assertThat(eventRepository.findByEventCode("DRAFT-2026").orElseThrow().getStatus().name()).isEqualTo("DRAFT");

        mockMvc.perform(patch("/api/events/{code}/publish", "DRAFT-2026").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void addArtist_adminOnly() throws Exception {
        Long artistId = artist("Solar Beat").getId();

        mockMvc.perform(post("/api/events/{c}/artists/{id}", "CMF-2026", artistId).header("Authorization", bearer(laura)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/{c}/artists/{id}", "CMF-2026", artistId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    // ---------- Tickets: propiedad / anti-IDOR ----------

    @Test
    @DisplayName("UC-SEC-04 USER compra para sí mismo -> 201")
    void purchase_ownEmail_returns201() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody("andrea@email.com")).header("Authorization", bearer(andrea)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"));
    }

    @Test
    @DisplayName("T-SEC-010 / UC-SEC-05 USER compra con el correo de otro -> 403 y 0 tickets creados")
    void purchase_foreignEmail_returns403_andCreatesNothing() throws Exception {
        long before = ticketRepository.count();

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody("carlos@email.com")).header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());

        assertThat(ticketRepository.count()).isEqualTo(before);
    }

    @Test
    void purchase_anonymous401_andStaffForbidden() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(purchaseBody("andrea@email.com")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody("laura@email.com")).header("Authorization", bearer(laura)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T-SEC-009 / UC-SEC-06 consultar ticket: propio 200, ajeno 403, STAFF/ADMIN 200")
    void findTicket_ownership() throws Exception {
        mockMvc.perform(get("/api/tickets/{c}", "TCK-ANDREA").header("Authorization", bearer(andrea)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{c}", "TCK-CARLOS").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ticketCode").doesNotExist());

        mockMvc.perform(get("/api/tickets/{c}", "TCK-CARLOS").header("Authorization", bearer(laura)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/{c}", "TCK-CARLOS").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Política anti-enumeración: ticket ajeno e inexistente dan el mismo 403 a un USER; STAFF recibe 404")
    void findTicket_foreignAndNonExistent_lookTheSameToUsers() throws Exception {
        mockMvc.perform(get("/api/tickets/{c}", "TCK-NO-EXISTE").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/{c}", "TCK-NO-EXISTE").header("Authorization", bearer(laura)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ticketsByUser_ownerAndAdminOnly() throws Exception {
        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com").header("Authorization", bearer(andrea)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-ANDREA"));
        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@email.com").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@email.com").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-011 cancelar: propio 200; ajeno 403 y el ticket NO cambia")
    void cancel_ownership() throws Exception {
        mockMvc.perform(patch("/api/tickets/{c}/cancel", "TCK-CARLOS").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        assertThat(ticketRepository.findByTicketCode("TCK-CARLOS").orElseThrow().getStatus()).isEqualTo(TicketStatus.PAID);

        mockMvc.perform(patch("/api/tickets/{c}/cancel", "TCK-ANDREA").header("Authorization", bearer(andrea)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_staffCannotCancelOthers_adminCan() throws Exception {
        mockMvc.perform(patch("/api/tickets/{c}/cancel", "TCK-CARLOS").header("Authorization", bearer(laura)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/{c}/cancel", "TCK-CARLOS").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-012/013 / UC-SEC-07 uso: USER 403; STAFF 200; segundo uso 409 (regla heredada)")
    void use_staffOnly_andSecondUseIsRejectedByBusinessRule() throws Exception {
        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-ANDREA").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        assertThat(ticketRepository.findByTicketCode("TCK-ANDREA").orElseThrow().getStatus()).isEqualTo(TicketStatus.PAID);

        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-ANDREA").header("Authorization", bearer(laura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-ANDREA").header("Authorization", bearer(laura)))
                .andExpect(status().isConflict());
    }

    @Test
    void paidTicketsOfEvent_staffAndAdminOnly() throws Exception {
        mockMvc.perform(get("/api/events/{c}/tickets/paid", "CMF-2026").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/{c}/tickets/paid", "CMF-2026").header("Authorization", bearer(laura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- Usuarios (T-SEC-015) ----------

    @Test
    void userLookups_ownerOrAdminOnly() throws Exception {
        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com").header("Authorization", bearer(andrea)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@email.com").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-username").param("username", "carlos").header("Authorization", bearer(andrea)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-username").param("username", "carlos").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@email.com"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Estado vigente (T-SEC-016, UC-SEC-10) ----------

    @Test
    @DisplayName("T-SEC-016 token válido de un usuario desactivado -> 401 en el siguiente request")
    void deactivatedUser_losesAccessImmediately() throws Exception {
        String token = bearer(carlos);
        mockMvc.perform(get("/api/auth/me").header("Authorization", token)).andExpect(status().isOk());

        carlos.deactivate();
        userRepository.saveAndFlush(carlos);

        mockMvc.perform(get("/api/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"carlos\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Revocar un rol surte efecto de inmediato: el token no conserva privilegios")
    void revokedRole_takesEffectOnNextRequest() throws Exception {
        String token = bearer(laura);
        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-ANDREA").header("Authorization", token)).andExpect(status().isOk());

        roleRepository.delete(UserRole.of(laura.getId(), Role.STAFF));
        roleRepository.flush();

        mockMvc.perform(patch("/api/tickets/{c}/use", "TCK-CARLOS").header("Authorization", token)).andExpect(status().isForbidden());
    }

    // ---------- Errores sin filtraciones (T-SEC-019) ----------

    @Test
    @DisplayName("T-SEC-019 las respuestas de error no exponen stacktrace, contraseña ni hash")
    void errorResponses_doNotLeakInternals() throws Exception {
        String[] bodies = {
                mockMvc.perform(get("/api/auth/me")).andReturn().getResponse().getContentAsString(),
                mockMvc.perform(get("/api/tickets/{c}", "TCK-CARLOS").header("Authorization", bearer(andrea)))
                        .andReturn().getResponse().getContentAsString(),
                mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\": \"andrea\", \"password\": \"mala-mala-mala\"}"))
                        .andReturn().getResponse().getContentAsString(),
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer basura"))
                        .andReturn().getResponse().getContentAsString()
        };
        for (String body : bodies) {
            assertThat(body).doesNotContain("com.pulsepass").doesNotContain("Exception").doesNotContain("$2a$")
                    .doesNotContain(PASSWORD).contains("\"timestamp\"").contains("\"status\"");
        }
    }

    @Test
    @DisplayName("Cada respuesta lleva X-Request-Id (trazabilidad)")
    void responses_carryRequestId() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(header().exists("X-Request-Id"));
        mockMvc.perform(get("/api/events/published").header("X-Request-Id", "mi-id-123"))
                .andExpect(header().string("X-Request-Id", "mi-id-123"));
        mockMvc.perform(get("/api/events/published").header("X-Request-Id", "id con espacios y <script>"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.not("id con espacios y <script>")));
    }
}
