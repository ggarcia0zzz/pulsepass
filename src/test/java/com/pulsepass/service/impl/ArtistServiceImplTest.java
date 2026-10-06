package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
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
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    @InjectMocks
    private ArtistServiceImpl service;

    private final Artist artist = Artist.create("Solar Beat", "Electronic", "Colombia");
    private final ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Electronic", "Colombia", true);

    @Test
    void findById_existingArtist_returnsResponse() {
        // ARRANGE
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        // ACT
        ArtistResponse result = service.findById(1L);

        // ASSERT
        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_unknownArtist_throwsResourceNotFound() {
        // ARRANGE
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT (BR-ARTIST-001)
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(artistMapper);
    }

    @Test
    void findByStageName_ignoresCaseAndSurroundingSpaces() {
        // ARRANGE
        when(artistRepository.findByStageNameIgnoreCase("solar beat")).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        // ACT
        ArtistResponse result = service.findByStageName("  solar beat ");

        // ASSERT
        assertThat(result).isEqualTo(response);
        verify(artistRepository).findByStageNameIgnoreCase("solar beat");
    }

    @Test
    void findByStageName_unknownArtist_throwsResourceNotFound() {
        // ARRANGE
        when(artistRepository.findByStageNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByStageName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findActiveArtists_usesTheActiveQuery() {
        // ARRANGE
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(artist));
        when(artistMapper.toResponse(any(Artist.class))).thenReturn(response);

        // ACT
        List<ArtistResponse> result = service.findActiveArtists();

        // ASSERT (BR-ARTIST-002)
        assertThat(result).containsExactly(response);
        verify(artistRepository).findByActiveTrueOrderByStageNameAsc();
    }
}
