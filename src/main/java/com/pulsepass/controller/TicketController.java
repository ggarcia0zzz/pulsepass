package com.pulsepass.controller;

import com.pulsepass.dto.TicketPurchaseRequest;
import com.pulsepass.dto.TicketResponse;
import com.pulsepass.security.AuthenticatedUser;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) { this.service = service; }

    @PostMapping("/api/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse purchase(@Valid @RequestBody TicketPurchaseRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.purchase(request, actor);
    }

    @GetMapping("/api/tickets/by-user")
    public List<TicketResponse> byUser(@RequestParam String email, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.listByUser(email, actor);
    }

    @GetMapping("/api/tickets/{ticketCode}")
    public TicketResponse byCode(@PathVariable String ticketCode, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.getByCode(ticketCode, actor);
    }

    @GetMapping("/api/events/{eventCode}/tickets/paid")
    public List<TicketResponse> paidByEvent(@PathVariable String eventCode) { return service.listPaidByEvent(eventCode); }

    @PatchMapping("/api/tickets/{ticketCode}/cancel")
    public TicketResponse cancel(@PathVariable String ticketCode, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.cancel(ticketCode, actor);
    }

    @PatchMapping("/api/tickets/{ticketCode}/use")
    public TicketResponse use(@PathVariable String ticketCode) { return service.use(ticketCode); }
}
