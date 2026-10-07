package com.feedstartup.service.impl;

import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmReview;
import com.feedstartup.repository.EpmReviewRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Moving a review up or down the EPM page's slider - the admin list is paged, so the backend reorders. */
class EpmReviewServiceImplTest {

    private final EpmReviewRepository repository = mock(EpmReviewRepository.class);
    private final EpmReviewServiceImpl service = new EpmReviewServiceImpl(repository);

    @Test
    void movingDownSwapsWithTheNextReviewAndRenumbersTiedOrders() {
        // Orders 0, 0, 0: ties stuck as created - moving still works and leaves them 0..n-1.
        List<EpmReview> reviews = List.of(review(1L, 0), review(2L, 0), review(3L, 0));
        when(repository.findAll(any(Sort.class))).thenReturn(reviews);

        service.move(1L, 1);

        assertEquals(List.of(1, 0, 2), reviews.stream().map(EpmReview::getDisplayOrder).toList());
        // Review 2 was already at 0, so only the two whose order changed are written.
        ArgumentCaptor<List<EpmReview>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertEquals(List.of(1L, 3L), saved.getValue().stream().map(EpmReview::getId).toList());
    }

    @Test
    void movingTheFirstReviewUpChangesNothing() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(review(1L, 0), review(2L, 1)));

        service.move(1L, -1);

        verify(repository, never()).saveAll(any());
    }

    @Test
    void anUnknownReviewOrABigJumpIsRefused() {
        when(repository.findAll(any(Sort.class))).thenReturn(new ArrayList<>(List.of(review(1L, 0))));

        assertThrows(ResourceNotFoundException.class, () -> service.move(9L, 1));
        assertThrows(IllegalArgumentException.class, () -> service.move(1L, 2));
    }

    private static EpmReview review(Long id, int order) {
        EpmReview r = new EpmReview();
        r.setId(id);
        r.setDisplayOrder(order);
        return r;
    }
}
