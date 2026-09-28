package com.feedstartup.service;

import com.feedstartup.dto.EpmReviewDto;
import com.feedstartup.dto.EpmReviewRequestDto;

import java.util.List;

/** Testimonials on the EPM page - written and curated by the admin. */
public interface EpmReviewService {

    /** Published reviews only, in display order. */
    List<EpmReviewDto> listPublished();

    /** Every review, published or not. */
    List<EpmReviewDto> listAll();

    EpmReviewDto create(EpmReviewRequestDto dto);

    EpmReviewDto update(Long id, EpmReviewRequestDto dto);

    void delete(Long id);
}
