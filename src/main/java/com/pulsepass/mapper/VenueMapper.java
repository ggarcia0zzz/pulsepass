package com.pulsepass.mapper;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
