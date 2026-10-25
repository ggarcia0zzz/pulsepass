package com.pulsepass.domain;

import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTransitionsTest {

    @ParameterizedTest
    @CsvSource({
            "DRAFT, PUBLISHED",
            "DRAFT, CANCELLED",
            "PUBLISHED, SOLD_OUT",
            "PUBLISHED, FINISHED",
            "PUBLISHED, CANCELLED",
            "SOLD_OUT, FINISHED",
            "SOLD_OUT, CANCELLED"
    })
    void eventAllowsTheseTransitions(EventStatus from, EventStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, SOLD_OUT",
            "DRAFT, FINISHED",
            "PUBLISHED, DRAFT",
            "CANCELLED, PUBLISHED",
            "FINISHED, CANCELLED"
    })
    void eventRejectsTheseTransitions(EventStatus from, EventStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void onlyPublishedIsSellable() {
        assertThat(EventStatus.PUBLISHED.isSellable()).isTrue();
        assertThat(EventStatus.DRAFT.isSellable()).isFalse();
        assertThat(EventStatus.SOLD_OUT.isSellable()).isFalse();
        assertThat(EventStatus.FINISHED.isSellable()).isFalse();
        assertThat(EventStatus.CANCELLED.isSellable()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "RESERVED, PAID",
            "RESERVED, CANCELLED",
            "PAID, USED",
            "PAID, CANCELLED"
    })
    void ticketAllowsTheseTransitions(TicketStatus from, TicketStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "RESERVED, USED",
            "CANCELLED, PAID",
            "USED, CANCELLED"
    })
    void ticketRejectsTheseTransitions(TicketStatus from, TicketStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void eventRejectsInvalidTransitionAtRuntime() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("EVT-1", "Festival", "desc",
                com.pulsepass.domain.enums.EventCategory.MUSIC,
                LocalDateTime.of(2027, 1, 1, 20, 0), 0, venue);

        assertThatThrownBy(() -> event.changeStatus(EventStatus.FINISHED))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void ticketRejectsInvalidTransitionAtRuntime() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("EVT-1", "Festival", "desc",
                com.pulsepass.domain.enums.EventCategory.MUSIC,
                LocalDateTime.of(2027, 1, 1, 20, 0), 0, venue);
        User user = User.create("ana", "ana@example.com");
        Ticket ticket = Ticket.create("TCK-1", com.pulsepass.domain.enums.TicketType.GENERAL,
                TicketStatus.RESERVED, new BigDecimal("50000"), LocalDateTime.now(), user, event);

        assertThatThrownBy(() -> ticket.markUsed())
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESERVED);
    }
}
