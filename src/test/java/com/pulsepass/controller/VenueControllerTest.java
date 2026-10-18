package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.VenueService;
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

@WebMvcTest(VenueController.class)
class VenueControllerTest extends SecuredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    private static VenueResponse marina() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1 # 2-3", 3, true);
    }

    @Test
    @DisplayName("TEST-CTRL-VEN-001 venue existente -> 200")
    void findByCode_existing_returns200() throws Exception {
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(marina());

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.active").value(true));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    @Test
    @DisplayName("TEST-CTRL-VEN-002 venue inexistente -> 404 con ErrorResponse")
    void findByCode_missing_returns404() throws Exception {
        when(venueService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Venue", "NOPE"));

        mockMvc.perform(get("/api/venues/{code}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: NOPE"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("TEST-CTRL-VEN-003 venues activos -> 200")
    void findActiveVenues_returns200() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(marina()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"));

        verify(venueService).findActiveVenues();
    }
}
