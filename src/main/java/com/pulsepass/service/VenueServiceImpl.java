package com.pulsepass.service;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.CreateVenueDto;
import com.pulsepass.dto.VenueDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
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
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;

    private final VenueMapper venueMapper;

    @Override
    @Transactional
    public VenueDto create(CreateVenueDto request) {

        String code = TextNormalizer.code(request.code());

        // FR-VEN-002: codigo unico
        if (venueRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Venue", "code", code);
        }

        // FR-VEN-003 (capacidad > 0) ya la valida @Positive en el DTO y el CHECK en Postgres

        Venue venue = Venue.create(
                code,
                TextNormalizer.text(request.name()),
                TextNormalizer.text(request.city()),
                TextNormalizer.text(request.address()),
                request.capacity()
        );

        return venueMapper.toDto(venueRepository.save(venue));
    }

    @Override
    public VenueDto findById(Long id) {
        return venueRepository
                .findById(id)
                .map(venueMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
    }

    @Override
    public VenueDto findByCode(String code) {
        String normalized = TextNormalizer.code(code);

        return venueRepository
                .findByCode(normalized)
                .map(venueMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", normalized));
    }

    @Override
    public List<VenueDto> findAll() {
        return venueRepository
                .findAll()
                .stream()
                .map(venueMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public VenueDto deactivate(Long id) {
        Venue venue = venueRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));

        venue.deactivate();

        return venueMapper.toDto(venue);
    }
}
