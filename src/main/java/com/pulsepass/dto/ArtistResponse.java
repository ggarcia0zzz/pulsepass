package com.pulsepass.dto;

import com.pulsepass.domain.Artist;

public record ArtistResponse(Long id, String stageName, String genre, String country, boolean active) {
    public static ArtistResponse from(Artist a) {
        return new ArtistResponse(a.getId(), a.getStageName(), a.getGenre(), a.getCountry(), a.isActive());
    }
}
