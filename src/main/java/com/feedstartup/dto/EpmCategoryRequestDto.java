package com.feedstartup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin create/update payload for a category. {@code label} defaults to the name when blank. */
public record EpmCategoryRequestDto(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
        @Size(max = 200, message = "Label must be at most 200 characters") String label,
        String color,
        Integer displayOrder) {}
