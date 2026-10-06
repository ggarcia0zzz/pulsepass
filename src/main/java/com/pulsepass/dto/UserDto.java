package com.pulsepass.dto;

public record UserDto(
        Long id,
        String username,
        String email,
        boolean active,
        UserProfileDto profile
) {
}
