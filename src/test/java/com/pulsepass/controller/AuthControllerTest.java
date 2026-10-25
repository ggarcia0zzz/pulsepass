package com.pulsepass.controller;

import com.pulsepass.domain.enums.Role;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends SecuredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String LOGIN_BODY = """
            {"identifier": "andrea", "password": "UnaFraseSeguraLarga2026!"}
            """;

    @Test
    @DisplayName("T-SEC-005 login correcto (anónimo) -> 200 con Bearer y expiresIn 900")
    void login_valid_returns200WithToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt.firmado.demo", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("jwt.firmado.demo"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("T-SEC-006 contraseña incorrecta -> 401 genérico con ErrorResponse")
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("Login con campos vacíos -> 400 sin invocar AuthService")
    void login_blankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.identifier").value("Identifier is required"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("GET /api/auth/me sin token -> 401")
    void me_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/auth/me autenticado -> 200 con información mínima del principal")
    void me_authenticated_returnsMinimalPrincipal() throws Exception {
        SecurityPrincipal principal = new SecurityPrincipal(5L, "laura", null, true, List.of(Role.USER, Role.STAFF));
        when(authService.me(any(SecurityPrincipal.class)))
                .thenReturn(new AuthenticatedUserResponse(5L, "laura", List.of("USER", "STAFF")));

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("laura"))
                .andExpect(jsonPath("$.roles[1]").value("STAFF"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }
}
