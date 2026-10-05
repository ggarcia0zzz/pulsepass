package com.pulsepass.domain.enums;

public enum EventStatus {
    DRAFT,
    PUBLISHED,
    SOLD_OUT,
    CANCELLED,
    FINISHED;

    public boolean canTransitionTo(EventStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED || target == CANCELLED;
            case PUBLISHED -> target == SOLD_OUT || target == FINISHED || target == CANCELLED;
            case SOLD_OUT -> target == FINISHED || target == CANCELLED;
            case CANCELLED, FINISHED -> false;
        };
    }

    public boolean isSellable() {
        return this == PUBLISHED;
    }
}
