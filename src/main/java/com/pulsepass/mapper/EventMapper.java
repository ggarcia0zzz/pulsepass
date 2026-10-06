package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.dto.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "artistStageNames", source = "artists")
    EventDto toDto(Event event);

    // usado por MapStruct para convertir cada elemento de Set<Artist> a String
    default String artistToStageName(Artist artist) {
        return artist.getStageName();
    }
}
