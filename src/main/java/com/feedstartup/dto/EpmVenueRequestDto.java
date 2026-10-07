package com.feedstartup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin create/update payload for a venue. */
public record EpmVenueRequestDto(
        @NotBlank(message = "Venue name is required") @Size(max = 255, message = "Venue name must be at most 255 characters") String name,
        @NotBlank(message = "State is required") @Size(max = 255, message = "State must be at most 255 characters") String state,
        @NotBlank(message = "District is required") @Size(max = 255, message = "District must be at most 255 characters") String district,
        @NotBlank(message = "Place is required") @Size(max = 255, message = "Place must be at most 255 characters") String city,
        @Size(max = 500, message = "Address must be at most 500 characters") String address) {}
