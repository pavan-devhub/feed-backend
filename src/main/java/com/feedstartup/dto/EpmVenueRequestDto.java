package com.feedstartup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin create/update payload for a venue. */
public record EpmVenueRequestDto(
        @NotBlank(message = "Venue name is required") String name,
        @NotBlank(message = "State is required") String state,
        @NotBlank(message = "District is required") String district,
        @NotBlank(message = "Place is required") String city,
        @Size(max = 500, message = "Address must be at most 500 characters") String address) {}
