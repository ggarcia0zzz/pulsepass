package com.pulsepass.exception;

import java.time.LocalDate;

public class UnderAgeException extends BusinessException {

    public UnderAgeException(LocalDate birthDate, int minimumAge) {
        super("Buyer born on %s does not meet the minimum age of %d".formatted(birthDate, minimumAge));
    }
}
