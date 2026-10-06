package com.pulsepass.mapper;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.VenueDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VenueMapper {

    VenueDto toDto(Venue venue);
}
