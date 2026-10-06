package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface TicketService {

    TicketResponse purchase(@Valid PurchaseTicketRequest request);

    TicketResponse findByCode(String ticketCode);

    List<TicketResponse> findByUserEmail(String email);

    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    TicketResponse cancel(String ticketCode);

    TicketResponse markAsUsed(String ticketCode);
}
