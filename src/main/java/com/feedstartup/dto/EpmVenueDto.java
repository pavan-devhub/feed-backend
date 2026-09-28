package com.feedstartup.dto;

import com.feedstartup.model.EpmVenue;

/** Admin read shape for a reusable venue. */
public record EpmVenueDto(Long id, String name, String state, String district, String city, String address) {

    public static EpmVenueDto from(EpmVenue v) {
        return new EpmVenueDto(v.getId(), v.getName(), v.getState(), v.getDistrict(), v.getCity(), v.getAddress());
    }
}
