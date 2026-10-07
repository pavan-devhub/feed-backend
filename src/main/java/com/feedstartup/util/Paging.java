package com.feedstartup.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Turns a list endpoint's {@code page} / {@code size} query parameters into a Pageable, the same
 * way for every paged list - admin and public alike.
 */
public final class Paging {

    /** The largest page any list hands out. */
    public static final int MAX_PAGE_SIZE = 100;

    private Paging() {}

    /** {@code page} is 0-based; out-of-range values are pulled back in. */
    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), sort);
    }
}
