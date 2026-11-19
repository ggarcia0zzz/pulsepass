package com.pulsepass.dto;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record EventResponse(
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        Integer minimumAge,
        String streamingUrl,
        VenueResponse venue,
        List<ArtistResponse> artists) {

    public static EventResponse from(Event e) {
        return new EventResponse(e.getEventCode(), e.getName(), e.getDescription(), e.getCategory(),
                e.getStatus(), e.getEventDate(), e.getMinimumAge(), e.getStreamingUrl(),
                VenueResponse.from(e.getVenue()),
                e.getArtists().stream()
                        .sorted(Comparator.comparing(Artist::getStageName))
                        .map(ArtistResponse::from).toList());
    }
}
