package com.pulsepass.repository;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-ART-001 a FR-ART-004, BR-003, AC-003, UC-03, QT-005.
 */
class EventArtistIT extends IntegrationTestBase {

    @Test
    void unEventoConTresArtistasPersisteLaAsociacion() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        Event event = saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        event.addArtist(artist("Solar Beat"));
        event.addArtist(artist("Neon Waves"));
        event.addArtist(artist("Caribbean Sound"));
        eventRepository.saveAndFlush(event);
        flushAndClear();

        Event reloaded = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        Set<String> stageNames = reloaded.getArtists().stream()
                .map(Artist::getStageName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("Solar Beat", "Neon Waves", "Caribbean Sound"), stageNames);
    }

    @Test
    void noDuplicaElMismoParEventoArtista() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        Event event = saveEvent("CMF-2026", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        Artist solarBeat = artist("Solar Beat");
        event.addArtist(solarBeat);
        event.addArtist(solarBeat);
        eventRepository.saveAndFlush(event);
        flushAndClear();

        Event reloaded = eventRepository.findByEventCode("CMF-2026").orElseThrow();
        assertEquals(1, reloaded.getArtists().size());

        // La PK compuesta (event_id, artist_id) impide repetir el par directamente en la BD.
        Long eventId = reloaded.getId();
        Long artistId = solarBeat.getId();
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO event_artists (event_id, artist_id) VALUES (?, ?)", eventId, artistId));
    }

    @Test
    void elArtistaVeSusEventosDesdeElLadoInverso() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        Artist solarBeat = artist("Solar Beat");
        Event first = saveEvent("EVT-001", venue, LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED);
        Event second = saveEvent("EVT-002", venue, LocalDateTime.now().plusDays(20), EventStatus.PUBLISHED);
        saveEvent("EVT-003", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED);
        first.addArtist(solarBeat);
        second.addArtist(solarBeat);
        flushAndClear();

        Artist reloaded = artistRepository.findByStageName("Solar Beat").orElseThrow();

        Set<String> eventCodes = reloaded.getEvents().stream()
                .map(Event::getEventCode)
                .collect(Collectors.toSet());
        assertEquals(Set.of("EVT-001", "EVT-002"), eventCodes);
    }
}