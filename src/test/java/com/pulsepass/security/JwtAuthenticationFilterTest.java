package com.pulsepass.security;

import com.pulsepass.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/tickets/TCK-1");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static SecurityPrincipal principal(boolean active) {
        return new SecurityPrincipal(7L, "andrea", null, active, List.of(Role.USER));
    }

    @Test
    void noAuthorizationHeader_continuesUnauthenticated() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void validBearer_buildsAuthenticationFromPersistedPrincipal() throws Exception {
        request.addHeader("Authorization", "Bearer good.jwt.token");
        when(jwtService.extractUserId("good.jwt.token")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal(true));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(SecurityPrincipal.class);
        assertThat(((SecurityPrincipal) authentication.getPrincipal()).getId()).isEqualTo(7L);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void bearerSchemeIsCaseInsensitive() throws Exception {
        request.addHeader("Authorization", "bearer good.jwt.token");
        when(jwtService.extractUserId("good.jwt.token")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal(true));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void invalidBearer_neverAuthenticates_butChainContinues() throws Exception {
        request.addHeader("Authorization", "Bearer bad.jwt.token");
        when(jwtService.extractUserId("bad.jwt.token")).thenThrow(new InvalidJwtException("Invalid token"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void validTokenOfUnknownUser_neverAuthenticates() throws Exception {
        request.addHeader("Authorization", "Bearer good.jwt.token");
        when(jwtService.extractUserId("good.jwt.token")).thenReturn(99L);
        when(userDetailsService.loadUserById(99L)).thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenOfDisabledUser_neverAuthenticates() throws Exception {
        request.addHeader("Authorization", "Bearer good.jwt.token");
        when(jwtService.extractUserId("good.jwt.token")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal(false));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void duplicatedAuthorizationHeader_isSuspicious_andNotAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer first.jwt.token");
        request.addHeader("Authorization", "Bearer second.jwt.token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void nonBearerScheme_isIgnored() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void oversizedToken_isRejectedBeforeParsing() throws Exception {
        request.addHeader("Authorization", "Bearer " + "a".repeat(5000));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        UsernamePasswordAuthenticationToken existing = UsernamePasswordAuthenticationToken.authenticated(
                "ya-autenticado", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer good.jwt.token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(jwtService, userDetailsService);
    }
}
