package com.pulsepass.service.impl;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private Venue venue() {
        return Venue.create("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 2-3", 3);
    }

    private VenueResponse response() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Cra 1 # 2-3", 3, true);
    }

    @Test
    void findByCode_existingVenue_returnsResponse() {
        // ARRANGE
        Venue venue = venue();
        VenueResponse expected = response();
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(expected);

        // ACT (el codigo se normaliza: espacios y minusculas)
        VenueResponse result = service.findByCode(" ven-smr-01 ");

        // ASSERT
        assertThat(result).isEqualTo(expected);
        verify(venueRepository).findByCode("VEN-SMR-01");
    }

    @Test
    void findByCode_unknownVenue_throwsResourceNotFound() {
        // ARRANGE
        when(venueRepository.findByCode("VEN-XXX")).thenReturn(Optional.empty());

        // ACT + ASSERT (BR-VENUE-001)
        assertThatThrownBy(() -> service.findByCode("VEN-XXX"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("VEN-XXX");

        verifyNoInteractions(venueMapper);
    }

    @Test
    void findActiveVenues_returnsOnlyWhatTheActiveQueryReturns() {
        // ARRANGE
        Venue venue = venue();
        VenueResponse expected = response();
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(any(Venue.class))).thenReturn(expected);

        // ACT
        List<VenueResponse> result = service.findActiveVenues();

        // ASSERT (BR-VENUE-002)
        assertThat(result).containsExactly(expected);
        verify(venueRepository).findByActiveTrueOrderByNameAsc();
    }
}
