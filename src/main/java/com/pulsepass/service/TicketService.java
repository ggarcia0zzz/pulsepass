package com.pulsepass.service;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.dto.IssueTicketDto;
import com.pulsepass.dto.TicketDto;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketService {

    TicketDto issue(@Valid IssueTicketDto request);

    TicketDto pay(Long ticketId);

    TicketDto cancel(Long ticketId);

    TicketDto markUsed(Long ticketId);

    TicketDto findById(Long id);

    TicketDto findByTicketCode(String ticketCode);

    List<TicketDto> findByUserEmail(String email);

    List<TicketDto> findByUserEmailAndStatus(String email, TicketStatus status);

    List<TicketDto> findPaidByEvent(String eventCode);

    long countPaidByEvent(String eventCode);

    List<TicketDto> findByEventDateAfter(LocalDateTime after);
}
