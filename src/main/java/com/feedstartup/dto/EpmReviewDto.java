package com.feedstartup.dto;

import com.feedstartup.model.EpmReview;

import java.time.LocalDateTime;

/** Read shape for a testimonial. The public endpoint only ever returns published ones. */
public record EpmReviewDto(Long id, String authorName, String authorRole, String content, int rating,
                           boolean published, int displayOrder, LocalDateTime createdAt) {

    public static EpmReviewDto from(EpmReview r) {
        return new EpmReviewDto(r.getId(), r.getAuthorName(), r.getAuthorRole(), r.getContent(), r.getRating(),
                r.isPublished(), r.getDisplayOrder(), r.getCreatedAt());
    }
}
