package com.pulsepass.exception;

import com.pulsepass.domain.enums.EventStatus;

public class EventNotSellableException extends BusinessException {

    public EventNotSellableException(String eventCode, EventStatus status) {
        super("Event %s is %s and is not selling tickets".formatted(eventCode, status));
    }
}
