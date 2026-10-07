package com.feedstartup.service;

import com.feedstartup.dto.EpmReviewDto;
import com.feedstartup.dto.EpmReviewPageDto;
import com.feedstartup.dto.EpmReviewRequestDto;

import java.util.List;

/** Testimonials on the EPM page - written and curated by the admin. */
public interface EpmReviewService {

    /** Published reviews only, in display order. */
    List<EpmReviewDto> listPublished();

    /**
     * The admin's review list - every review, published or not, in display order - a page at a time
     * ({@code page} 0-based), with how many of them the EPM page shows.
     */
    EpmReviewPageDto pageAll(int page, int size);

    /**
     * Moves a review one place up ({@code delta} -1) or down (+1) in the display order. Every
     * review's order is rewritten as 0..n-1 on the way, so ties can't stick; moving the first one
     * up or the last one down changes nothing.
     */
    void move(Long id, int delta);

    EpmReviewDto create(EpmReviewRequestDto dto);

    EpmReviewDto update(Long id, EpmReviewRequestDto dto);

    void delete(Long id);
}
