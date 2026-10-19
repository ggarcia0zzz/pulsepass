package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final long ANDREA = 1L;
    private static final long CARLOS = 2L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketRepository ticketRepository;

    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        service = new AuthorizationService(userRepository, ticketRepository);

        User andrea = user(ANDREA, "andrea", "andrea@email.com");
        User carlos = user(CARLOS, "carlos", "carlos@email.com");
        lenient().when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea));
        lenient().when(userRepository.findByEmailIgnoreCase("carlos@email.com")).thenReturn(Optional.of(carlos));
        lenient().when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        lenient().when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(carlos));
        lenient().when(ticketRepository.findOwnerIdByTicketCode("TCK-ANDREA")).thenReturn(Optional.of(ANDREA));
        lenient().when(ticketRepository.findOwnerIdByTicketCode("TCK-CARLOS")).thenReturn(Optional.of(CARLOS));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static User user(long id, String username, String email) {
        User user = User.create(username, email);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static void loginAs(long id, Role... roles) {
        SecurityPrincipal principal = new SecurityPrincipal(id, "u" + id, null, true, List.of(roles));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    // ---------- anónimo ----------

    @Test
    void anonymous_isDeniedEverywhere() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(service.currentPrincipal()).isEmpty();
        assertThat(service.canViewTicket("TCK-ANDREA")).isFalse();
        assertThat(service.canPurchaseFor("andrea@email.com")).isFalse();
        assertThat(service.canViewUnpublishedEvents()).isFalse();
        assertThat(service.canViewEvent(EventStatus.PUBLISHED)).isTrue(); // público
        assertThat(service.canViewEvent(EventStatus.DRAFT)).isFalse();
    }

    // ---------- compra ----------

    @Test
    void purchase_ownEmail_isAllowed_otherUsersEmail_isDenied() {
        loginAs(ANDREA, Role.USER);

        assertThat(service.canPurchaseFor("andrea@email.com")).isTrue();
        assertThat(service.canPurchaseFor(" ANDREA@email.com ")).isTrue();
        assertThat(service.canPurchaseFor("carlos@email.com")).isFalse();
        assertThat(service.canPurchaseFor("nadie@email.com")).isFalse();
        assertThat(service.canPurchaseFor(null)).isFalse();
    }

    @Test
    void purchase_requiresRoleUser() {
        loginAs(ANDREA, Role.STAFF);

        assertThat(service.canPurchaseFor("andrea@email.com")).isFalse();
    }

    // ---------- tickets ----------

    @Test
    void viewTicket_ownerYes_thirdPartyNo_staffAndAdminYes() {
        loginAs(ANDREA, Role.USER);
        assertThat(service.canViewTicket("TCK-ANDREA")).isTrue();
        assertThat(service.canViewTicket("tck-andrea")).isTrue();
        assertThat(service.canViewTicket("TCK-CARLOS")).isFalse();
        assertThat(service.canViewTicket("TCK-NO-EXISTE")).isFalse(); // misma respuesta que ajeno

        loginAs(9L, Role.STAFF);
        assertThat(service.canViewTicket("TCK-CARLOS")).isTrue();

        loginAs(10L, Role.ADMIN);
        assertThat(service.canViewTicket("TCK-CARLOS")).isTrue();
    }

    @Test
    void cancelTicket_ownerYes_thirdPartyNo_adminYes_staffNo() {
        loginAs(ANDREA, Role.USER);
        assertThat(service.canCancelTicket("TCK-ANDREA")).isTrue();
        assertThat(service.canCancelTicket("TCK-CARLOS")).isFalse();

        loginAs(9L, Role.STAFF);
        assertThat(service.canCancelTicket("TCK-CARLOS")).isFalse();

        loginAs(10L, Role.ADMIN);
        assertThat(service.canCancelTicket("TCK-CARLOS")).isTrue();
    }

    @Test
    void ticketsByUserEmail_ownerOrAdminOnly() {
        loginAs(ANDREA, Role.USER);
        assertThat(service.canViewTicketsOfEmail("andrea@email.com")).isTrue();
        assertThat(service.canViewTicketsOfEmail("carlos@email.com")).isFalse();

        loginAs(9L, Role.STAFF);
        assertThat(service.canViewTicketsOfEmail("carlos@email.com")).isFalse();

        loginAs(10L, Role.ADMIN);
        assertThat(service.canViewTicketsOfEmail("carlos@email.com")).isTrue();
    }

    // ---------- usuarios ----------

    @Test
    void userLookups_ownerOrAdminOnly() {
        loginAs(ANDREA, Role.USER);
        assertThat(service.canViewUserByEmail("andrea@email.com")).isTrue();
        assertThat(service.canViewUserByEmail("carlos@email.com")).isFalse();
        assertThat(service.canViewUserByUsername("andrea")).isTrue();
        assertThat(service.canViewUserByUsername("carlos")).isFalse();

        loginAs(10L, Role.ADMIN);
        assertThat(service.canViewUserByEmail("carlos@email.com")).isTrue();
        assertThat(service.canViewUserByUsername("carlos")).isTrue();
    }

    // ---------- eventos ----------

    @Test
    void unpublishedEvents_visibleOnlyToStaffAndAdmin() {
        loginAs(ANDREA, Role.USER);
        assertThat(service.canViewEvent(EventStatus.DRAFT)).isFalse();
        assertThat(service.canViewEvent(EventStatus.PUBLISHED)).isTrue();

        loginAs(9L, Role.STAFF);
        assertThat(service.canViewEvent(EventStatus.DRAFT)).isTrue();

        loginAs(10L, Role.ADMIN);
        assertThat(service.canViewEvent(EventStatus.CANCELLED)).isTrue();
    }
}
