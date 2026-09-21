package com.pulsepass.repository;

import com.pulsepass.IntegrationTestBase;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-SRC-001, FR-SRC-002, FR-SRC-003, AC-007, UC-07, UC-09, QT-008.
 */
class EventSearchIT extends IntegrationTestBase {

    private Event saveEventWithArtists(String eventCode, Venue venue, LocalDateTime date,
                                       EventStatus status, String... stageNames) {
        Event event = saveEvent(eventCode, venue, date, status);
        for (String stageName : stageNames) {
            event.addArtist(artist(stageName));
        }
        return eventRepository.saveAndFlush(event);
    }

    private List<String> codesOf(List<Event> events) {
        return events.stream().map(Event::getEventCode).toList();
    }

    @Test
    void buscaEventosPorArtistaYCadaEventoApareceUnaSolaVez() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        saveEventWithArtists("EVT-001", venue, LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED,
                "Solar Beat", "Neon Waves", "Caribbean Sound");
        saveEventWithArtists("EVT-002", venue, LocalDateTime.now().plusDays(20), EventStatus.PUBLISHED,
                "Solar Beat");
        saveEventWithArtists("EVT-003", venue, LocalDateTime.now().plusDays(30), EventStatus.PUBLISHED,
                "Neon Waves");
        flushAndClear();

        List<String> codes = codesOf(eventRepository.findByArtistStageName("Solar Beat"));

        assertEquals(2, codes.size());
        assertEquals(Set.of("EVT-001", "EVT-002"), Set.copyOf(codes));
    }

    @Test
    void buscaEventosPorCiudadYArtista() {
        Venue santaMarta = saveVenue("VEN-SMR-01", "Santa Marta");
        Venue bogota = saveVenue("VEN-BOG-01", "Bogota");
        saveEventWithArtists("EVT-SMR", santaMarta, LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED,
                "Solar Beat");
        saveEventWithArtists("EVT-BOG", bogota, LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED,
                "Solar Beat");
        saveEventWithArtists("EVT-SMR-OTHER", santaMarta, LocalDateTime.now().plusDays(12), EventStatus.PUBLISHED,
                "Ocean Drive");
        flushAndClear();

        List<String> codes = codesOf(eventRepository.findByCityAndArtistStageName("Santa Marta", "Solar Beat"));

        assertEquals(List.of("EVT-SMR"), codes);
    }

    @Test
    void eventosRecomendadosAplicanTodosLosFiltrosYOrdenanPorFecha() {
        Venue santaMarta = saveVenue("VEN-SMR-01", "Santa Marta");
        Venue bogota = saveVenue("VEN-BOG-01", "Bogota");
        LocalDateTime now = LocalDateTime.now();

        // Los dos que deben aparecer (el cercano primero)
        saveEventWithArtists("EVT-FAR", santaMarta, now.plusDays(10), EventStatus.PUBLISHED, "Solar Beat");
        saveEventWithArtists("EVT-NEAR", santaMarta, now.plusDays(5), EventStatus.PUBLISHED, "Solar Beat");
        // Los que deben quedar fuera
        saveEventWithArtists("EVT-DRAFT", santaMarta, now.plusDays(6), EventStatus.DRAFT, "Solar Beat");
        saveEventWithArtists("EVT-CANCELLED", santaMarta, now.plusDays(7), EventStatus.CANCELLED, "Solar Beat");
        saveEventWithArtists("EVT-PAST", santaMarta, now.minusDays(5), EventStatus.PUBLISHED, "Solar Beat");
        saveEventWithArtists("EVT-BOG", bogota, now.plusDays(8), EventStatus.PUBLISHED, "Solar Beat");
        saveEventWithArtists("EVT-OTHER-ARTIST", santaMarta, now.plusDays(9), EventStatus.PUBLISHED, "Ocean Drive");
        flushAndClear();

        // "sOlAr" comprueba que el texto del artista no distingue mayusculas
        List<String> codes = codesOf(eventRepository.findRecommended(now, "Santa Marta", "sOlAr"));

        assertEquals(List.of("EVT-NEAR", "EVT-FAR"), codes);
    }

    @Test
    void eventosRecomendadosNoRepitenUnEventoConVariosArtistasCoincidentes() {
        Venue venue = saveVenue("VEN-SMR-01", "Santa Marta");
        LocalDateTime now = LocalDateTime.now();
        // Los tres artistas contienen la letra "e": sin DISTINCT el evento saldria 3 veces
        saveEventWithArtists("CMF-2026", venue, now.plusDays(30), EventStatus.PUBLISHED,
                "Solar Beat", "Neon Waves", "Caribbean Sound");
        flushAndClear();

        List<String> codes = codesOf(eventRepository.findRecommended(now, "Santa Marta", "e"));

        assertEquals(List.of("CMF-2026"), codes);
    }
}