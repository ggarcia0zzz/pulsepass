package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.AssignArtistDto;
import com.pulsepass.dto.ChangeEventStatusDto;
import com.pulsepass.dto.CreateEventDto;
import com.pulsepass.dto.EventDto;
import com.pulsepass.dto.SetStreamingUrlDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class EventServiceImpl implements EventService {

    // OJO: el orden de estos campos define el orden del constructor (@RequiredArgsConstructor)
    private final EventRepository eventRepository;

    private final VenueRepository venueRepository;

    private final ArtistRepository artistRepository;

    private final EventMapper eventMapper;

    @Override
    @Transactional
    public EventDto create(CreateEventDto request) {

        String eventCode = TextNormalizer.code(request.eventCode());
        String venueCode = TextNormalizer.code(request.venueCode());

        // FR-EVT-002: eventCode unico
        if (eventRepository.existsByEventCode(eventCode)) {
            throw new DuplicateResourceException("Event", "eventCode", eventCode);
        }

        // BR-001: todo Event pertenece a exactamente un Venue existente
        Venue venue = venueRepository
                .findByCode(venueCode)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueCode));

        // Regla derivada: un venue desactivado no puede recibir eventos nuevos
        if (!venue.isActive()) {
            throw new ResourceNotFoundException("Venue", venueCode);
        }

        Event event = Event.create(
                eventCode,
                TextNormalizer.text(request.name()),
                TextNormalizer.optionalText(request.description()),
                request.category(),
                request.eventDate(),
                request.minimumAge(),
                venue
        );
        // Event.create() ya inicia en DRAFT (ver Event.java)

        return eventMapper.toDto(eventRepository.save(event));
    }

    @Override
    public EventDto findById(Long id) {
        return eventMapper.toDto(findEvent(id));
    }

    @Override
    public EventDto findByEventCode(String eventCode) {
        String normalized = TextNormalizer.code(eventCode);

        return eventRepository
                .findByEventCode(normalized)
                .map(eventMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Event", normalized));
    }

    @Override
    public List<EventDto> findPublished() {
        return eventRepository
                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Override
    public List<EventDto> findByVenue(String venueCode) {
        String code = TextNormalizer.code(venueCode);

        if (!venueRepository.existsByCode(code)) {
            throw new ResourceNotFoundException("Venue", code);
        }

        return eventRepository
                .findByVenue_Code(code)
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Override
    public List<EventDto> findByArtist(String stageName) {
        return eventRepository
                .findByArtistStageName(TextNormalizer.text(stageName))
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Override
    public List<EventDto> findByCityAndArtist(String city, String stageName) {
        return eventRepository
                .findByCityAndArtistStageName(TextNormalizer.text(city), TextNormalizer.text(stageName))
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Override
    public List<EventDto> findRecommended(LocalDateTime after, String city, String artistText) {
        return eventRepository
                .findRecommended(after, TextNormalizer.text(city), TextNormalizer.text(artistText))
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventDto assignArtist(Long eventId, AssignArtistDto request) {

        Event event = findEvent(eventId);

        // FR-ART-003: el artista debe existir; Set<Artist> evita duplicar el par evento-artista
        Artist artist = artistRepository
                .findByStageName(TextNormalizer.text(request.stageName()))
                .orElseThrow(() -> new ResourceNotFoundException("Artist", request.stageName()));

        event.addArtist(artist);

        return eventMapper.toDto(event);
    }

    @Override
    @Transactional
    public EventDto changeStatus(Long eventId, ChangeEventStatusDto request) {

        Event event = findEvent(eventId);

        // La entidad valida la transicion (ver EventStatus.canTransitionTo) y lanza
        // InvalidStatusTransitionException si no es un paso valido del flujo
        event.changeStatus(request.newStatus());

        return eventMapper.toDto(event);
    }

    @Override
    @Transactional
    public EventDto setStreamingUrl(Long eventId, SetStreamingUrlDto request) {

        Event event = findEvent(eventId);
        event.setStreamingUrl(TextNormalizer.text(request.streamingUrl()));

        return eventMapper.toDto(event);
    }

    private Event findEvent(Long id) {
        return eventRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }
}
