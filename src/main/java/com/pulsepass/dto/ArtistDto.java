package com.pulsepass.dto;

public record ArtistDto(
        Long id,
        String stageName,
        String genre,
        String country,
        boolean active
) {
}
