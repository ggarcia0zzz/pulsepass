package com.pulsepass.controller;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.InvalidStatusTransitionException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest extends SecuredControllerTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 12, 20, 20, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private static EventResponse event(EventStatus status, Set<ArtistResponse> artists) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026", "Festival",
                EventCategory.MUSIC, status, DATE, 18, null,
                "VEN-SMR-01", "Marina Convention Center", artists);
    }

    private static EventSummaryResponse summary() {
        return new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, DATE,
                "VEN-SMR-01", "Marina Convention Center");
    }

    private static final String VALID_BODY = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival",
              "category": "MUSIC",
              "eventDate": "2026-12-20T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    @Test
    @DisplayName("TEST-CTRL-EVT-001 crear valido -> 201")
    void create_valid_returns201() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(event(EventStatus.DRAFT, Set.of()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-002 request invalido -> 400 con details y sin invocar Service")
    void create_invalid_returns400() throws Exception {
        String invalid = """
                {
                  "eventCode": "",
                  "name": "Caribbean Music Fest 2026",
                  "category": "MUSIC",
                  "eventDate": "2026-12-20T20:00:00",
                  "minimumAge": 18
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("minimumAge negativo -> 400")
    void create_negativeMinimumAge_returns400() throws Exception {
        String body = VALID_BODY.replace("\"minimumAge\": 18", "\"minimumAge\": -1");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age cannot be negative"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("JSON mal formado -> 400 sin invocar Service")
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("Regla de negocio al crear (venue inactivo, fecha pasada...) -> 409")
    void create_businessRule_returns409() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new BusinessRuleException("Venue is not active: VEN-SMR-01"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-003 consultar existente -> 200")
    void findByCode_existing_returns200() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(event(EventStatus.DRAFT, Set.of()));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.eventDate").value("2026-12-20T20:00:00"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-004 inexistente -> 404")
    void findByCode_missing_returns404() throws Exception {
        when(eventService.findByCode("NOPE")).thenThrow(new ResourceNotFoundException("Event", "NOPE"));

        mockMvc.perform(get("/api/events/{eventCode}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found: NOPE"));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-005 listar publicados -> 200")
    void findPublishedEvents_returns200() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
        verify(eventService, never()).findByCode(any());
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-006 publicar valido -> 200")
    void publish_valid_returns200() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(event(EventStatus.PUBLISHED, Set.of()));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-007 publicar invalido -> 409")
    void publish_invalidTransition_returns409() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new InvalidStatusTransitionException(EventStatus.CANCELLED, EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Invalid status transition: CANCELLED -> PUBLISHED"));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-008 asociar artista -> 200")
    void addArtist_valid_returns200() throws Exception {
        ArtistResponse solar = new ArtistResponse(1L, "Solar Beat", "Electronic", "Colombia", true);
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(event(EventStatus.DRAFT, Set.of(solar)));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @DisplayName("Asociar artista duplicado -> 409")
    void addArtist_conflict_returns409() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenThrow(new BusinessRuleException("Artist already associated with event: CMF-2026"));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Asociar artista inexistente -> 404")
    void addArtist_missingArtist_returns404() throws Exception {
        when(eventService.addArtist("CMF-2026", 99L))
                .thenThrow(new ResourceNotFoundException("Artist", 99L));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-009 buscar por artista -> 200")
    void findByArtist_returns200() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test
    @DisplayName("by-artist sin stageName -> 400 sin invocar Service")
    void findByArtist_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/events/by-artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(eventService, never()).findByArtist(any());
    }

    @Test
    @DisplayName("Error inesperado -> 500 sin exponer detalles internos")
    void unexpectedError_returns500() throws Exception {
        when(eventService.findByCode("BOOM")).thenThrow(new IllegalStateException("db password=secret"));

        mockMvc.perform(get("/api/events/{eventCode}", "BOOM"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected internal error"));
    }
}
