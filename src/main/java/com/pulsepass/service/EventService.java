package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.EventCreateRequest;
import com.pulsepass.dto.EventResponse;
import com.pulsepass.exception.ConflictException;
import com.pulsepass.exception.NotFoundException;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EventService {
    private final EventRepository events;
    private final VenueRepository venues;
    private final ArtistRepository artists;

    public EventService(EventRepository events, VenueRepository venues, ArtistRepository artists) {
        this.events = events;
        this.venues = venues;
        this.artists = artists;
    }

    /** La autorizacion ADMIN la aplica SecurityConfig. */
    public EventResponse create(EventCreateRequest req) {
        String code = req.eventCode().trim();
        if (events.findByEventCode(code).isPresent()) throw new ConflictException("El codigo de evento ya existe");
        Venue venue = venues.findByCode(req.venueCode().trim())
                .orElseThrow(() -> new NotFoundException("Sede no encontrada"));
        if (!venue.isActive()) throw new ConflictException("La sede esta inactiva");
        Event event = Event.create(code, req.name().trim(), req.description(), req.category(),
                req.eventDate(), req.minimumAge(), venue);
        if (req.streamingUrl() != null && !req.streamingUrl().isBlank()) event.setStreamingUrl(req.streamingUrl().trim());
        return EventResponse.from(events.saveAndFlush(event));
    }

    /** Borrador: solo STAFF/ADMIN. Para el resto responde 404 como si no existiera (no se filtra nada). */
    @Transactional(readOnly = true)
    public EventResponse getByCode(String eventCode, AuthenticatedUser actor) {
        Event event = events.findByEventCode(eventCode).orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        if (event.getStatus() == EventStatus.DRAFT && (actor == null || !actor.isOps())) {
            throw new NotFoundException("Evento no encontrado");
        }
        return EventResponse.from(event);
    }

    /** Cartelera publica: PUBLISHED y SOLD_OUT. Nunca borradores. */
    @Transactional(readOnly = true)
    public List<EventResponse> listPublished() {
        return events.findByStatusInOrderByEventDateAsc(List.of(EventStatus.PUBLISHED, EventStatus.SOLD_OUT))
                .stream().map(EventResponse::from).toList();
    }

    /** Listado administrativo de todos los estados. SecurityConfig exige ADMIN. */
    @Transactional(readOnly = true)
    public List<EventResponse> listAll() {
        return events.findAll(org.springframework.data.domain.Sort.by("eventDate").ascending())
                .stream().map(EventResponse::from).toList();
    }

    public EventResponse publish(String eventCode) {
        Event event = events.findByEventCode(eventCode).orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        if (event.getStatus() != EventStatus.DRAFT) throw new ConflictException("Solo se puede publicar un evento en borrador");
        if (!event.getEventDate().isAfter(LocalDateTime.now())) throw new ConflictException("La fecha del evento ya paso");
        event.publish();
        return EventResponse.from(events.saveAndFlush(event));
    }

    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = events.findByEventCode(eventCode).orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        Artist artist = artists.findById(artistId).orElseThrow(() -> new NotFoundException("Artista no encontrado"));
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new ConflictException("El evento ya no admite cambios");
        }
        if (!artist.isActive()) throw new ConflictException("El artista esta inactivo");
        if (event.getArtists().contains(artist)) throw new ConflictException("El artista ya esta asociado al evento");
        event.addArtist(artist);
        return EventResponse.from(events.saveAndFlush(event));
    }

    /** Publico: solo PUBLISHED. STAFF/ADMIN: todos los estados. */
    @Transactional(readOnly = true)
    public List<EventResponse> findByArtist(String stageName, AuthenticatedUser actor) {
        boolean ops = actor != null && actor.isOps();
        return events.findByArtistStageName(stageName).stream()
                .filter(e -> ops || e.getStatus() == EventStatus.PUBLISHED)
                .sorted((a, b) -> a.getEventDate().compareTo(b.getEventDate()))
                .map(EventResponse::from).toList();
    }
}
