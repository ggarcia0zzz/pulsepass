package com.pulsepass.domain.enums;

public enum TicketStatus {
    RESERVED,
    PAID,
    CANCELLED,
    USED;

    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case RESERVED -> target == PAID || target == CANCELLED;
            case PAID -> target == USED || target == CANCELLED;
            case CANCELLED, USED -> false;
        };
    }
}
