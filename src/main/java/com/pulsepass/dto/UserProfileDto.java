package com.pulsepass.dto;

import java.time.LocalDate;

public record UserProfileDto(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate
) {
}
