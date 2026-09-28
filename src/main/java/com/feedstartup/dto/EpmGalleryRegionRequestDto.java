package com.feedstartup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin create/rename payload for a gallery state or district. */
public record EpmGalleryRegionRequestDto(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
        Integer displayOrder) {}
