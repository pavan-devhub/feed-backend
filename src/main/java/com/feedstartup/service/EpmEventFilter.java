package com.feedstartup.service;

import com.feedstartup.model.EpmEvent;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which EPMs a list shows - the public EPM directory, the register / volunteer pages and the
 * admin's EPM Events screen. Every filter is optional and they combine. The database applies them,
 * so the lists can be paged there (see EpmEventService#page).
 *
 * @param status           UPCOMING (today or later), PREVIOUS (before today) or ALL
 * @param query            free-text match on title, place, district, state, venue or category
 * @param state            exact match, ignoring case - likewise {@code district}, {@code city} and
 *                         {@code category} (a category name)
 * @param month            1-12, the EPM's calendar month - in every year unless {@code year} is set
 * @param year             the EPM's calendar year
 * @param includeCancelled also list EPMs the admin has cancelled
 */
public record EpmEventFilter(Status status, String query, String state, String district, String city,
                             String category, Integer month, Integer year, boolean includeCancelled) {

    public enum Status {
        UPCOMING, PREVIOUS, ALL;

        /** "upcoming" (also for null or blank), "previous" or "all", in any case. */
        public static Status parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return UPCOMING;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "upcoming" -> UPCOMING;
                case "previous" -> PREVIOUS;
                case "all" -> ALL;
                default -> throw new IllegalArgumentException("Invalid status filter: " + raw);
            };
        }
    }

    private static final List<String> SEARCHED = List.of("title", "city", "district", "state", "venue", "category");

    public EpmEventFilter {
        if (status == null) {
            status = Status.UPCOMING;
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
    }

    /**
     * The public lists, which leave cancelled EPMs out - except upcoming ones when
     * {@code includeCancelled}: the register / volunteer pages list those, marked cancelled.
     */
    public static EpmEventFilter forPublic(String status, String query, String state, String district, String city,
                                           String category, Integer month, Integer year, boolean includeCancelled) {
        Status parsed = Status.parse(status);
        return new EpmEventFilter(parsed, query, state, district, city, category, month, year,
                includeCancelled && parsed == Status.UPCOMING);
    }

    /** The admin's lists, cancelled EPMs included. */
    public static EpmEventFilter forAdmin(String status, String query, String state, String district, String city,
                                          String category, Integer month, Integer year) {
        return new EpmEventFilter(Status.parse(status), query, state, district, city, category, month, year, true);
    }

    /** The same status and cancelled rule with every other filter cleared - a whole tab of the list. */
    public EpmEventFilter statusOnly() {
        return new EpmEventFilter(status, null, null, null, null, null, null, null, includeCancelled);
    }

    /** {@code today} decides which EPMs are upcoming and which are previous. */
    public Specification<EpmEvent> toSpecification(LocalDate today) {
        return (root, cq, cb) -> {
            List<Predicate> where = new ArrayList<>();
            Path<LocalDate> eventDate = root.get("eventDate");
            switch (status) {
                case UPCOMING -> where.add(cb.greaterThanOrEqualTo(eventDate, today));
                case PREVIOUS -> where.add(cb.lessThan(eventDate, today));
                case ALL -> { }
            }
            if (!includeCancelled) {
                where.add(cb.isFalse(root.get("cancelled")));
            }
            if (hasText(query)) {
                String like = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
                where.add(cb.or(SEARCHED.stream()
                        .map(field -> cb.like(cb.lower(root.<String>get(field)), like, '\\'))
                        .toArray(Predicate[]::new)));
            }
            addEqualsIgnoringCase(where, root, cb, "state", state);
            addEqualsIgnoringCase(where, root, cb, "district", district);
            addEqualsIgnoringCase(where, root, cb, "city", city);
            addEqualsIgnoringCase(where, root, cb, "category", category);
            if (month != null) {
                where.add(cb.equal(cb.function("month", Integer.class, eventDate), month));
            }
            if (year != null) {
                where.add(cb.equal(cb.function("year", Integer.class, eventDate), year));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /** Upcoming EPMs soonest first; previous ones (and all) latest first. The id breaks ties so paging is stable. */
    public Sort sort() {
        return status == Status.UPCOMING
                ? Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("eventDate"), Sort.Order.desc("id"));
    }

    private static void addEqualsIgnoringCase(List<Predicate> where, Root<EpmEvent> root,
                                              CriteriaBuilder cb, String field, String value) {
        if (hasText(value)) {
            where.add(cb.equal(cb.lower(root.<String>get(field)), value.trim().toLowerCase(Locale.ROOT)));
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // So a search for "50%" or "a_b" matches those characters literally.
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
