package com.pulsepass.service.impl;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.ArtistService;
import com.pulsepass.shared.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    @Override
    public ArtistResponse findById(Long id) {
        // BR-ARTIST-001: si el artista no existe -> ResourceNotFoundException
        return artistRepository
                .findById(id)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", id));
    }

    @Override
    public ArtistResponse findByStageName(String stageName) {
        String normalized = TextNormalizer.text(stageName);

        return artistRepository
                .findByStageNameIgnoreCase(normalized)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", normalized));
    }

    @Override
    public List<ArtistResponse> findActiveArtists() {
        // BR-ARTIST-002: solo artistas activos
        return artistRepository
                .findByActiveTrueOrderByStageNameAsc()
                .stream()
                .map(artistMapper::toResponse)
                .toList();
    }
}
