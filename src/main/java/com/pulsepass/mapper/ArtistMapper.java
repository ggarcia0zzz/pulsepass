package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.ArtistDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ArtistMapper {

    ArtistDto toDto(Artist artist);
}
