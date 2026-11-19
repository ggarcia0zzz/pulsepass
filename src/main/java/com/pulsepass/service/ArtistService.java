package com.pulsepass.service;

import com.pulsepass.dto.ArtistResponse;
import com.pulsepass.exception.NotFoundException;
import com.pulsepass.repository.ArtistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArtistService {
    private final ArtistRepository artists;

    public ArtistService(ArtistRepository artists) { this.artists = artists; }

    public List<ArtistResponse> listActive() {
        return artists.findByActiveTrue().stream().map(ArtistResponse::from).toList();
    }

    public ArtistResponse getById(Long id) {
        return artists.findById(id).map(ArtistResponse::from)
                .orElseThrow(() -> new NotFoundException("Artista no encontrado"));
    }

    public ArtistResponse getByStageName(String stageName) {
        return artists.findByStageName(stageName).map(ArtistResponse::from)
                .orElseThrow(() -> new NotFoundException("Artista no encontrado"));
    }
}
