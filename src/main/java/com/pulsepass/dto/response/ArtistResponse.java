package com.pulsepass.dto.response;

public record ArtistResponse(
        Long id,
        String stageName,
        String genre,
        String country,
        boolean active
) {
}
