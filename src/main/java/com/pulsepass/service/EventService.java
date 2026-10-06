package com.pulsepass.service;

import com.pulsepass.dto.AssignArtistDto;
import com.pulsepass.dto.ChangeEventStatusDto;
import com.pulsepass.dto.CreateEventDto;
import com.pulsepass.dto.EventDto;
import com.pulsepass.dto.SetStreamingUrlDto;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventDto create(@Valid CreateEventDto request);

    EventDto findById(Long id);

    EventDto findByEventCode(String eventCode);

    List<EventDto> findPublished();

    List<EventDto> findByVenue(String venueCode);

    List<EventDto> findByArtist(String stageName);

    List<EventDto> findByCityAndArtist(String city, String stageName);

    List<EventDto> findRecommended(LocalDateTime after, String city, String artistText);

    EventDto assignArtist(Long eventId, @Valid AssignArtistDto request);

    EventDto changeStatus(Long eventId, @Valid ChangeEventStatusDto request);

    EventDto setStreamingUrl(Long eventId, @Valid SetStreamingUrlDto request);
}
