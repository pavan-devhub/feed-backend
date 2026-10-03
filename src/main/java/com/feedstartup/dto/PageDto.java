package com.feedstartup.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of an admin list. {@code page} is 0-based; {@code total} counts every matching row
 * across all pages.
 */
public record PageDto<T>(List<T> items, long total, int page, int size, int totalPages) {

    public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                page.getNumber(), page.getSize(), page.getTotalPages());
    }
}
