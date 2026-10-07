package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmReviewDto;
import com.feedstartup.dto.EpmReviewPageDto;
import com.feedstartup.dto.EpmReviewRequestDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmReview;
import com.feedstartup.repository.EpmReviewRepository;
import com.feedstartup.service.EpmReviewService;
import com.feedstartup.util.Paging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EpmReviewServiceImpl implements EpmReviewService {

    /** The admin list's order, the same as the EPM page's; the id breaks ties so paging is stable. */
    private static final Sort ADMIN_ORDER = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final EpmReviewRepository reviewRepository;

    @Autowired
    public EpmReviewServiceImpl(EpmReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<EpmReviewDto> listPublished() {
        return reviewRepository.findByPublishedTrueOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(EpmReviewDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public EpmReviewPageDto pageAll(int page, int size) {
        return new EpmReviewPageDto(
                PageDto.of(reviewRepository.findAll(Paging.of(page, size, ADMIN_ORDER)), EpmReviewDto::from),
                reviewRepository.countByPublishedTrue());
    }

    @Override
    @Transactional
    public void move(Long id, int delta) {
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("A review moves one place at a time - up (-1) or down (1)");
        }
        List<EpmReview> ordered = new ArrayList<>(reviewRepository.findAll(ADMIN_ORDER));
        int from = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(id)) {
                from = i;
                break;
            }
        }
        if (from == -1) {
            throw new ResourceNotFoundException("Review not found: " + id);
        }
        int to = from + delta;
        if (to < 0 || to >= ordered.size()) {
            return;
        }
        ordered.add(to, ordered.remove(from));
        List<EpmReview> changed = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            EpmReview review = ordered.get(i);
            if (review.getDisplayOrder() != i) {
                review.setDisplayOrder(i);
                changed.add(review);
            }
        }
        reviewRepository.saveAll(changed);
    }

    @Override
    public EpmReviewDto create(EpmReviewRequestDto dto) {
        EpmReview review = new EpmReview();
        apply(review, dto);
        if (dto.displayOrder() == null) {
            review.setDisplayOrder(reviewRepository.maxDisplayOrder() + 1);
        }
        return EpmReviewDto.from(reviewRepository.save(review));
    }

    @Override
    public EpmReviewDto update(Long id, EpmReviewRequestDto dto) {
        EpmReview review = findOrThrow(id);
        apply(review, dto);
        return EpmReviewDto.from(reviewRepository.save(review));
    }

    @Override
    public void delete(Long id) {
        reviewRepository.delete(findOrThrow(id));
    }

    private static void apply(EpmReview review, EpmReviewRequestDto dto) {
        review.setAuthorName(dto.authorName().trim());
        review.setAuthorRole(dto.authorRole() == null || dto.authorRole().isBlank() ? null : dto.authorRole().trim());
        review.setContent(dto.content().trim());
        review.setRating(dto.rating() != null ? dto.rating() : 5);
        if (dto.published() != null) review.setPublished(dto.published());
        if (dto.displayOrder() != null) review.setDisplayOrder(dto.displayOrder());
    }

    private EpmReview findOrThrow(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }
}
