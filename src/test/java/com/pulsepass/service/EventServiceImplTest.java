package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.AssignArtistDto;
import com.pulsepass.dto.ChangeEventStatusDto;
import com.pulsepass.dto.CreateEventDto;
import com.pulsepass.dto.EventDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.InvalidStatusTransitionException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl service;

    private CreateEventDto request() {
        return new CreateEventDto(
                " cmf-2026 ", "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(60), 18, " ven-smr-01 ");
    }

    @Test
    void shouldCreateEventInDraftStatus() {
        var request = request();
        Venue venue = Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1", 5000);
        var expected = new EventDto(1L, "CMF-2026", "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                EventStatus.DRAFT, request.eventDate(), 18, null, "VEN-SMR-01",
                "Marina Convention Center", java.util.Set.of());

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toDto(any(Event.class))).thenReturn(expected);

        EventDto result = service.create(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(captor.getValue().getVenue()).isSameAs(venue);
    }

    @Test
    void shouldRejectDuplicatedEventCode() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());
        verifyNoInteractions(venueRepository);
    }

    @Test
    void shouldRejectUnknownVenue() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveVenue() {
        Venue venue = Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1", 5000);
        venue.deactivate();

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldAssignArtistToEvent() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("EVT-1", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, venue);
        Artist artist = Artist.create("Solar Beat", "Electronic", "Colombia");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(artistRepository.findByStageName("Solar Beat")).thenReturn(Optional.of(artist));
        when(eventMapper.toDto(event)).thenReturn(null);

        service.assignArtist(1L, new AssignArtistDto("Solar Beat"));

        assertThat(event.getArtists()).containsExactly(artist);
    }

    @Test
    void shouldRejectAssigningUnknownArtist() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("EVT-1", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, venue);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(artistRepository.findByStageName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignArtist(1L, new AssignArtistDto("Unknown")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldPublishEvent() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("EVT-1", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, venue);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toDto(event)).thenReturn(null);

        service.changeStatus(1L, new ChangeEventStatusDto(EventStatus.PUBLISHED));

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);
        Event event = Event.create("EVT-1", "Festival", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, venue);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() ->
                service.changeStatus(1L, new ChangeEventStatusDto(EventStatus.FINISHED)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void shouldRejectFindByVenueWhenVenueDoesNotExist() {
        when(venueRepository.existsByCode("VEN-XXX")).thenReturn(false);

        assertThatThrownBy(() -> service.findByVenue("ven-xxx"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(eventRepository);
    }
}
