package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.ArtistDto;
import com.pulsepass.dto.RegisterArtistDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    @InjectMocks
    private ArtistServiceImpl service;

    @Test
    void shouldRegisterArtist() {
        var request = new RegisterArtistDto(" Solar Beat ", "Electronic", "Colombia");
        var expected = new ArtistDto(1L, "Solar Beat", "Electronic", "Colombia", true);

        when(artistRepository.existsByStageName("Solar Beat")).thenReturn(false);
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artistMapper.toDto(any(Artist.class))).thenReturn(expected);

        ArtistDto result = service.register(request);

        assertThat(result).isEqualTo(expected);
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void shouldRejectDuplicatedStageName() {
        var request = new RegisterArtistDto("Solar Beat", "Electronic", "Colombia");

        when(artistRepository.existsByStageName("Solar Beat")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(artistRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenArtistDoesNotExist() {
        when(artistRepository.findByStageName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByStageName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
