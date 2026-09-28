package com.feedstartup.dto;

import jakarta.validation.constraints.Size;

/** Admin edit payload for an image's metadata. Every field is optional - null leaves it unchanged,
 * and an empty string clears a text field. */
public record EpmGalleryImageUpdateDto(
        @Size(max = 500, message = "Caption must be at most 500 characters") String caption,
        String city,
        String state,
        Boolean featured,
        Integer displayOrder) {}
