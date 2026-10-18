package com.pulsepass.controller;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.InvalidStatusTransitionException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

@WebMvcTest(TicketController.class)
class TicketControllerTest extends SecuredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_BODY = """
            {
              "userEmail": "andrea@email.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    private static TicketResponse ticket(TicketStatus status) {
        return new TicketResponse(1L, "TCK-0001", TicketType.GENERAL, new BigDecimal("100000.00"),
                status, LocalDateTime.of(2026, 10, 1, 10, 0),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // ---------- purchase ----------

    @Test
    @DisplayName("TEST-CTRL-TKT-001 compra valida -> 201")
    void purchase_valid_returns201() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.price").value(100000.00))
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-002 request invalido -> 400 sin invocar Service")
    void purchase_invalid_returns400() throws Exception {
        String invalid = """
                { "userEmail": "no-es-email", "eventCode": "" }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email must be valid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("Tipo de ticket inexistente en el enum -> 400")
    void purchase_unknownType_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("GENERAL", "PLATINUM")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-003 usuario inexistente -> 404")
    void purchase_userNotFound_returns404() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User", "andrea@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: andrea@email.com"));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-004 regla de negocio (menor de edad) -> 409")
    void purchase_businessRule_returns409() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age"));
    }

    // ---------- queries ----------

    @Test
    @DisplayName("TEST-CTRL-TKT-005 consultar ticket -> 200")
    void findByCode_returns200() throws Exception {
        when(ticketService.findByCode("TCK-0001")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"));

        verify(ticketService).findByCode("TCK-0001");
    }

    @Test
    @DisplayName("Consultar ticket inexistente -> 404")
    void findByCode_missing_returns404() throws Exception {
        when(ticketService.findByCode("NOPE")).thenThrow(new ResourceNotFoundException("Ticket", "NOPE"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: NOPE"));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-006 tickets por usuario -> 200")
    void findByUserEmail_returns200() throws Exception {
        when(ticketService.findByUserEmail("andrea@email.com")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@email.com"));

        verify(ticketService).findByUserEmail("andrea@email.com");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-007 tickets PAID por evento -> 200")
    void findPaidTicketsByEvent_returns200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    // ---------- cancel / use ----------

    @Test
    @DisplayName("TEST-CTRL-TKT-008 cancelar valido -> 200")
    void cancel_valid_returns200() throws Exception {
        when(ticketService.cancel("TCK-0001")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-0001");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-009 cancelar invalido (USED) -> 409")
    void cancel_invalid_returns409() throws Exception {
        when(ticketService.cancel("TCK-0001"))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.USED, TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Invalid status transition: USED -> CANCELLED"));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-010 usar valido -> 200")
    void markAsUsed_valid_returns200() throws Exception {
        when(ticketService.markAsUsed("TCK-0001")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-0001");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-011 usar invalido (CANCELLED) -> 409")
    void markAsUsed_invalid_returns409() throws Exception {
        when(ticketService.markAsUsed("TCK-0001"))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.CANCELLED, TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
