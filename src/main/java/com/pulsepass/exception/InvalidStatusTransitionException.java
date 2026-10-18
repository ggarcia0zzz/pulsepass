package com.pulsepass.exception;

public class InvalidStatusTransitionException extends BusinessRuleException {

    public <S> InvalidStatusTransitionException(S from, S to) {
        super("Invalid status transition: %s -> %s".formatted(from, to));
    }
}
