package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    // "Ahora" congelado: 2026-09-28
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime FUTURE_DATE = LocalDateTime.of(2026, 12, 5, 20, 0);
    private static final LocalDateTime PAST_DATE = LocalDateTime.of(2026, 9, 1, 20, 0);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper eventMapper;

    private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventServiceImpl(eventRepository, venueRepository, artistRepository, eventMapper, CLOCK);
    }

    // ---------- helpers ----------

    private Venue activeVenue() {
        return Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 2-3", 3);
    }

    private Event draftEvent(Venue venue, LocalDateTime date) {
        return Event.create("CMF-2026", "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC, date, 18, venue);
    }

    private CreateEventRequest request(LocalDateTime date, Integer minimumAge) {
        return new CreateEventRequest(" cmf-2026 ", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, date, minimumAge, " ven-smr-01 ");
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                status, FUTURE_DATE, 18, null, "VEN-SMR-01", "Marina Convention Center", Set.of());
    }

    // ---------- findByCode ----------

    @Test
    void findByCode_existingEvent_returnsResponse() {
        // ARRANGE (TEST-EVENT-001)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = service.findByCode("cmf-2026");

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findByCode_unknownEvent_throwsResourceNotFound() {
        // ARRANGE (TEST-EVENT-002)
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByCode("CMF-2026"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: CMF-2026");

        verifyNoInteractions(eventMapper);
    }

    // ---------- create ----------

    @Test
    void create_validEvent_savesEventInDraft() {
        // ARRANGE (TEST-EVENT-003 / AC-001)
        Venue venue = activeVenue();
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse result = service.create(request(FUTURE_DATE, 18));

        // ASSERT
        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EventStatus.DRAFT);   // BR-EVENT-005
        assertThat(saved.getEventCode()).isEqualTo("CMF-2026");
        assertThat(saved.getVenue()).isSameAs(venue);
        assertThat(saved.getMinimumAge()).isEqualTo(18);
    }

    @Test
    void create_nullMinimumAge_isTreatedAsNoRestriction() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(activeVenue()));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response(EventStatus.DRAFT));

        // ACT
        service.create(request(FUTURE_DATE, null));

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getMinimumAge()).isZero();
    }

    @Test
    void create_duplicatedEventCode_throwsDuplicateAndDoesNotSave() {
        // ARRANGE (BR-EVENT-001)
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE_DATE, 18)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());
        verifyNoInteractions(venueRepository);
    }

    @Test
    void create_unknownVenue_throwsResourceNotFoundAndDoesNotSave() {
        // ARRANGE (TEST-EVENT-004)
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE_DATE, 18)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("VEN-SMR-01");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_inactiveVenue_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-EVENT-005)
        Venue venue = activeVenue();
        venue.deactivate();
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE_DATE, 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_pastDate_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (TEST-EVENT-006)
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(activeVenue()));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(PAST_DATE, 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_negativeMinimumAge_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE (BR-EVENT-006)
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(activeVenue()));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE_DATE, -1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Minimum age");

        verify(eventRepository, never()).save(any());
    }

    // ---------- findPublishedEvents / findByArtist ----------

    @Test
    void findPublishedEvents_queriesPublishedStatusAndMapsToSummaries() {
        // ARRANGE
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        EventSummaryResponse summary = new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, FUTURE_DATE, "VEN-SMR-01", "Marina Convention Center");
        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(eventMapper.toSummary(event)).thenReturn(summary);

        // ACT
        List<EventSummaryResponse> result = service.findPublishedEvents();

        // ASSERT
        assertThat(result).containsExactly(summary);
        verify(eventRepository).findByStatusOrderByEventDateAsc(eq(EventStatus.PUBLISHED));
    }

    @Test
    void findByArtist_mapsEventsOfThatArtistToSummaries() {
        // ARRANGE
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        EventSummaryResponse summary = new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, FUTURE_DATE, "VEN-SMR-01", "Marina Convention Center");
        when(eventRepository.findByArtistStageName("Solar Beat")).thenReturn(List.of(event));
        when(eventMapper.toSummary(event)).thenReturn(summary);

        // ACT
        List<EventSummaryResponse> result = service.findByArtist(" Solar Beat ");

        // ASSERT
        assertThat(result).containsExactly(summary);
    }

    // ---------- publish ----------

    @Test
    void publish_validDraftEvent_becomesPublished() {
        // ARRANGE (TEST-EVENT-007 / AC-002)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        EventResponse expected = response(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = service.publish("CMF-2026");

        // ASSERT
        assertThat(result).isEqualTo(expected);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    @Test
    void publish_cancelledEvent_throwsBusinessRuleAndDoesNotPersist() {
        // ARRANGE (TEST-EVENT-008)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        event.cancel();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_alreadyPublishedEvent_throwsBusinessRule() {
        // ARRANGE (BR-EVENT-007)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        event.publish();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_eventWithPastDate_throwsBusinessRule() {
        // ARRANGE (BR-EVENT-008)
        Event event = draftEvent(activeVenue(), PAST_DATE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_eventWhoseVenueBecameInactive_throwsBusinessRule() {
        // ARRANGE (BR-EVENT-009)
        Venue venue = activeVenue();
        Event event = draftEvent(venue, FUTURE_DATE);
        venue.deactivate();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");

        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_unknownEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    // ---------- addArtist (FR-SVC-007) ----------

    @Test
    void addArtist_validArtist_associatesAndSaves() {
        // ARRANGE (AC-003)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        Artist artist = Artist.create("Solar Beat", "Electronic", "Colombia");
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = service.addArtist("CMF-2026", 1L);

        // ASSERT
        assertThat(result).isEqualTo(expected);
        assertThat(event.getArtists()).containsExactly(artist);
        verify(eventRepository).save(event);
    }

    @Test
    void addArtist_unknownArtist_throwsResourceNotFound() {
        // ARRANGE
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist("CMF-2026", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_unknownEvent_throwsResourceNotFoundWithoutLookingForTheArtist() {
        // ARRANGE
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(artistRepository);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_sameArtistTwice_throwsBusinessRule() {
        // ARRANGE (BR-EVENT-010)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        Artist artist = Artist.create("Solar Beat", "Electronic", "Colombia");
        event.addArtist(artist);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already associated");

        assertThat(event.getArtists()).hasSize(1);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_cancelledEvent_throwsBusinessRule() {
        // ARRANGE (BR-EVENT-011)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        event.cancel();
        Artist artist = Artist.create("Solar Beat", "Electronic", "Colombia");
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        assertThat(event.getArtists()).isEmpty();
        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_finishedEvent_throwsBusinessRule() {
        // ARRANGE (BR-EVENT-011)
        Event event = draftEvent(activeVenue(), FUTURE_DATE);
        event.publish();
        event.finish();
        Artist artist = Artist.create("Solar Beat", "Electronic", "Colombia");
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("FINISHED");

        verify(eventRepository, never()).save(any());
    }
}
