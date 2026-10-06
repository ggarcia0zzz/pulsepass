package com.pulsepass.mapper;

import com.pulsepass.domain.Ticket;
import com.pulsepass.dto.TicketDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TicketMapper {

    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "eventCode", source = "event.eventCode")
    TicketDto toDto(Ticket ticket);
}
