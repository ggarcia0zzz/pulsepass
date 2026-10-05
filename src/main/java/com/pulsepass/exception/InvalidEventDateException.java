package com.pulsepass.exception;

public class InvalidEventDateException extends BusinessException {

    public InvalidEventDateException(String reason) {
        super(reason);
    }
}
