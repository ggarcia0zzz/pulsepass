package com.pulsepass.service;

import com.pulsepass.dto.CreateVenueDto;
import com.pulsepass.dto.VenueDto;
import jakarta.validation.Valid;

import java.util.List;

public interface VenueService {

    VenueDto create(@Valid CreateVenueDto request);

    VenueDto findById(Long id);

    VenueDto findByCode(String code);

    List<VenueDto> findAll();

    VenueDto deactivate(Long id);
}
