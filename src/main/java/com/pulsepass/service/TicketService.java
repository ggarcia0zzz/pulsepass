package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface TicketService {

    @PreAuthorize("hasRole('USER') and @authorizationService.canPurchaseFor(#request.userEmail())")
    TicketResponse purchase(@Valid PurchaseTicketRequest request);

    @PreAuthorize("@authorizationService.canViewTicket(#ticketCode)")
    TicketResponse findByCode(String ticketCode);

    @PreAuthorize("@authorizationService.canViewTicketsOfEmail(#email)")
    List<TicketResponse> findByUserEmail(String email);

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    @PreAuthorize("@authorizationService.canCancelTicket(#ticketCode)")
    TicketResponse cancel(String ticketCode);

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    TicketResponse markAsUsed(String ticketCode);
}
