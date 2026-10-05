package com.pulsepass.exception;

public class ProfileAlreadyExistsException extends BusinessException {

    public ProfileAlreadyExistsException(Long userId) {
        super("User already has a profile: " + userId);
    }
}
