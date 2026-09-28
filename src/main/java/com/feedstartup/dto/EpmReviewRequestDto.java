package com.feedstartup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin create/update payload for a testimonial. */
public record EpmReviewRequestDto(
        @NotBlank(message = "Reviewer name is required") @Size(max = 255, message = "Name is too long") String authorName,
        @Size(max = 255, message = "Role is too long") String authorRole,
        @NotBlank(message = "Review text is required") @Size(max = 2000, message = "Review must be at most 2000 characters") String content,
        @Min(value = 1, message = "Rating must be between 1 and 5") @Max(value = 5, message = "Rating must be between 1 and 5") Integer rating,
        Boolean published,
        Integer displayOrder) {}
