package com.pulsepass.service;

import com.pulsepass.dto.ArtistDto;
import com.pulsepass.dto.RegisterArtistDto;
import jakarta.validation.Valid;

import java.util.List;

public interface ArtistService {

    ArtistDto register(@Valid RegisterArtistDto request);

    ArtistDto findById(Long id);

    ArtistDto findByStageName(String stageName);

    List<ArtistDto> findAll();

    ArtistDto deactivate(Long id);
}
