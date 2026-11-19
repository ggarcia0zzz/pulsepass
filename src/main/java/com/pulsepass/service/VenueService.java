package com.pulsepass.service;

import com.pulsepass.dto.VenueResponse;
import com.pulsepass.exception.NotFoundException;
import com.pulsepass.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VenueService {
    private final VenueRepository venues;

    public VenueService(VenueRepository venues) { this.venues = venues; }

    public List<VenueResponse> listActive() {
        return venues.findByActiveTrue().stream().map(VenueResponse::from).toList();
    }

    public VenueResponse getByCode(String code) {
        return venues.findByCode(code).map(VenueResponse::from)
                .orElseThrow(() -> new NotFoundException("Sede no encontrada"));
    }
}
