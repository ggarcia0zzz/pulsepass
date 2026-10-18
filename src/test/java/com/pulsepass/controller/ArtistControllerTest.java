package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
class ArtistControllerTest extends SecuredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    private static ArtistResponse solarBeat() {
        return new ArtistResponse(1L, "Solar Beat", "Electronic", "Colombia", true);
    }

    @Test
    @DisplayName("TEST-CTRL-ART-001 buscar por ID -> 200")
    void findById_existing_returns200() throws Exception {
        when(artistService.findById(1L)).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    @Test
    @DisplayName("TEST-CTRL-ART-002 ID inexistente -> 404")
    void findById_missing_returns404() throws Exception {
        when(artistService.findById(99L)).thenThrow(new ResourceNotFoundException("Artist", 99L));

        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test
    @DisplayName("ID no numerico -> 400")
    void findById_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/artists/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("TEST-CTRL-ART-003 buscar por stageName -> 200")
    void findByStageName_returns200() throws Exception {
        when(artistService.findByStageName("Solar Beat")).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    @DisplayName("stageName inexistente -> 404")
    void findByStageName_missing_returns404() throws Exception {
        when(artistService.findByStageName("Nadie"))
                .thenThrow(new ResourceNotFoundException("Artist", "Nadie"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Nadie"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: Nadie"));
    }

    @Test
    @DisplayName("stageName ausente -> 400")
    void findByStageName_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("TEST-CTRL-ART-004 listar activos -> 200")
    void findActiveArtists_returns200() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(
                solarBeat(),
                new ArtistResponse(2L, "Neon Waves", "Pop", "Colombia", true)));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"));

        verify(artistService).findActiveArtists();
    }
}
