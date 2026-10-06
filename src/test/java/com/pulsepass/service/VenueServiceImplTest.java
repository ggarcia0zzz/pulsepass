package com.pulsepass.service;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.CreateVenueDto;
import com.pulsepass.dto.VenueDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueMapper venueMapper;

    @InjectMocks
    private VenueServiceImpl service;

    @Test
    void shouldCreateVenueWithNormalizedCode() {
        var request = new CreateVenueDto(" ven-smr-01 ", " Marina Convention Center ", " Santa Marta ",
                " Cra 1 # 2-3 ", 5000);
        var expected = new VenueDto(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Cra 1 # 2-3", 5000, true);

        when(venueRepository.existsByCode("VEN-SMR-01")).thenReturn(false);
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));
        when(venueMapper.toDto(any(Venue.class))).thenReturn(expected);

        VenueDto result = service.create(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);
        verify(venueRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("VEN-SMR-01");
        assertThat(captor.getValue().getCapacity()).isEqualTo(5000);
    }

    @Test
    void shouldRejectDuplicatedCode() {
        var request = new CreateVenueDto("VEN-SMR-01", "Centro", "Santa Marta", "Cra 1", 100);

        when(venueRepository.existsByCode("VEN-SMR-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(venueRepository, never()).save(any());
        verifyNoInteractions(venueMapper);
    }

    @Test
    void shouldThrowWhenVenueDoesNotExist() {
        when(venueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(venueMapper);
    }

    @Test
    void shouldDeactivateVenue() {
        Venue venue = Venue.create("VEN-1", "Centro", "Santa Marta", "Cra 1", 100);

        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(venueMapper.toDto(venue)).thenReturn(null);

        service.deactivate(1L);

        assertThat(venue.isActive()).isFalse();
    }
}
