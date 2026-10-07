package com.feedstartup.dto;

/**
 * One page of the admin's review list, plus how many reviews - on any page - the EPM page's
 * Testimonials slider shows.
 */
public record EpmReviewPageDto(PageDto<EpmReviewDto> page, long publishedCount) {}
