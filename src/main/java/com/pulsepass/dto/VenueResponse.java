package com.pulsepass.dto;

import com.pulsepass.domain.Venue;

public record VenueResponse(String code, String name, String city, String address, Integer capacity, boolean active) {
    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getCode(), v.getName(), v.getCity(), v.getAddress(), v.getCapacity(), v.isActive());
    }
}
