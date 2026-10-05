package com.pulsepass.exception;

public class InactiveUserException extends BusinessException {

    public InactiveUserException(Long userId) {
        super("User is inactive and cannot buy tickets: " + userId);
    }
}
