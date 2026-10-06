package com.pulsepass.dto;

public record VenueDto(
        Long id,
        String code,
        String name,
        String city,
        String address,
        Integer capacity,
        boolean active
) {
}
