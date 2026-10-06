package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.ArtistDto;
import com.pulsepass.dto.RegisterArtistDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;

    private final ArtistMapper artistMapper;

    @Override
    @Transactional
    public ArtistDto register(RegisterArtistDto request) {

        String stageName = TextNormalizer.text(request.stageName());

        // FR-ART-002: stageName unico
        if (artistRepository.existsByStageName(stageName)) {
            throw new DuplicateResourceException("Artist", "stageName", stageName);
        }

        Artist artist = Artist.create(
                stageName,
                TextNormalizer.text(request.genre()),
                TextNormalizer.text(request.country())
        );

        return artistMapper.toDto(artistRepository.save(artist));
    }

    @Override
    public ArtistDto findById(Long id) {
        return artistRepository
                .findById(id)
                .map(artistMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", id));
    }

    @Override
    public ArtistDto findByStageName(String stageName) {
        return artistRepository
                .findByStageName(TextNormalizer.text(stageName))
                .map(artistMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", stageName));
    }

    @Override
    public List<ArtistDto> findAll() {
        return artistRepository
                .findAll()
                .stream()
                .map(artistMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ArtistDto deactivate(Long id) {
        Artist artist = artistRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", id));

        artist.deactivate();

        return artistMapper.toDto(artist);
    }
}
