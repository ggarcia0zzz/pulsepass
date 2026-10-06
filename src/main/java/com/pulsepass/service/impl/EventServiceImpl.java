package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;
import com.pulsepass.shared.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;
    private final Clock clock;

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper,
                            Clock clock) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        String eventCode = TextNormalizer.code(request.eventCode());
        String venueCode = TextNormalizer.code(request.venueCode());

        // BR-EVENT-001: no puede existir otro evento con el mismo eventCode
        if (eventRepository.existsByEventCode(eventCode)) {
            throw new DuplicateResourceException("Event", "eventCode", eventCode);
        }

        // BR-EVENT-002: el venue debe existir
        Venue venue = venueRepository
                .findByCode(venueCode)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueCode));

        // BR-EVENT-003: no se puede crear un evento en un venue inactivo
        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is inactive and cannot host events: " + venueCode);
        }

        // BR-EVENT-004: la fecha del evento debe ser futura
        if (!isFuture(request.eventDate())) {
            throw new BusinessRuleException("Event date must be in the future: " + request.eventDate());
        }

        // BR-EVENT-006: minimumAge >= 0 (0 = sin restriccion; null se interpreta como 0)
        int minimumAge = request.minimumAge() == null ? 0 : request.minimumAge();
        if (minimumAge < 0) {
            throw new BusinessRuleException("Minimum age must be greater than or equal to 0: " + minimumAge);
        }

        // BR-EVENT-005: Event.create() siempre inicia en DRAFT; el request no controla el estado
        Event event = Event.create(
                eventCode,
                TextNormalizer.text(request.name()),
                TextNormalizer.optionalText(request.description()),
                request.category(),
                request.eventDate(),
                minimumAge,
                venue
        );

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventMapper.toResponse(findEvent(eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository
                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {

        Event event = findEvent(eventCode);

        // BR-EVENT-007: solo se publica un evento en estado DRAFT
        // (PUBLISHED, SOLD_OUT, CANCELLED y FINISHED no se pueden publicar)
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Event %s is %s"
                            .formatted(event.getEventCode(), event.getStatus()));
        }

        // BR-EVENT-008: el evento debe seguir teniendo fecha futura
        if (!isFuture(event.getEventDate())) {
            throw new BusinessRuleException(
                    "Event %s cannot be published: its date is no longer in the future"
                            .formatted(event.getEventCode()));
        }

        // BR-EVENT-009: el venue debe continuar activo
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException(
                    "Event %s cannot be published: venue %s is inactive"
                            .formatted(event.getEventCode(), event.getVenue().getCode()));
        }

        event.publish(); // DRAFT -> PUBLISHED

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {

        Event event = findEvent(eventCode);

        Artist artist = artistRepository
                .findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", artistId));

        // BR-EVENT-011: no se agregan artistas a eventos CANCELLED o FINISHED
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Artists cannot be added to event %s because it is %s"
                            .formatted(event.getEventCode(), event.getStatus()));
        }

        // BR-EVENT-010: no se asocia dos veces el mismo artista al evento
        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException(
                    "Artist %s is already associated with event %s"
                            .formatted(artist.getStageName(), event.getEventCode()));
        }

        event.addArtist(artist);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository
                .findByArtistStageName(TextNormalizer.text(stageName))
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    private Event findEvent(String eventCode) {
        String normalized = TextNormalizer.code(eventCode);

        return eventRepository
                .findByEventCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Event", normalized));
    }

    private boolean isFuture(LocalDateTime date) {
        return date.isAfter(LocalDateTime.now(clock));
    }
}
