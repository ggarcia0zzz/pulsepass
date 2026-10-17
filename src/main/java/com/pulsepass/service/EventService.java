package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface EventService {

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse create(@Valid CreateEventRequest request);

    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse publish(String eventCode);

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse addArtist(String eventCode, Long artistId);

    List<EventSummaryResponse> findByArtist(String stageName);
}
