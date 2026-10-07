package com.feedstartup.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list. {@code page} is 0-based; {@code total} counts every matching row across all
 * pages.
 */
public record PageDto<T>(List<T> items, long total, int page, int size, int totalPages) {

    public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
        return of(page, page.getContent().stream().map(mapper).toList());
    }

    /** {@code page}'s rows already mapped - e.g. all at once, to look up related data in one query. */
    public static <T> PageDto<T> of(Page<?> page, List<T> items) {
        return new PageDto<>(items, page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages());
    }
}
