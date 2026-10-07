package com.feedstartup.service;

import com.feedstartup.util.Paging;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The admin's filters on the EPM Registrations and Volunteers screens. Every one is optional and
 * they combine. Both kinds of submission share these columns, so one filter serves both.
 *
 * @param epmEventId  only submissions for this EPM
 * @param eventDate   only submissions for EPMs held on this date
 * @param submittedOn only submissions made on this date
 * @param query       free-text match on name, mobile number, email, state, district, the EPM's city
 *                    or the participant type
 */
public record EpmSubmissionFilter(Long epmEventId, LocalDate eventDate, LocalDate submittedOn, String query) {

    /** Newest first; the id breaks ties so paging is stable. */
    public static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private static final List<String> SEARCHED = List.of("fullName", "mobileNumber", "email", "state", "district", "eventCity");

    public <T> Specification<T> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (epmEventId != null) {
                where.add(cb.equal(root.get("epmEventId"), epmEventId));
            }
            if (eventDate != null) {
                where.add(cb.equal(root.<LocalDate>get("eventDate"), eventDate));
            }
            if (submittedOn != null) {
                where.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), submittedOn.atStartOfDay()));
                where.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), submittedOn.plusDays(1).atStartOfDay()));
            }
            if (this.query != null && !this.query.isBlank()) {
                String like = "%" + escapeLike(this.query.trim().toLowerCase(Locale.ROOT)) + "%";
                List<Predicate> anyOf = new ArrayList<>(SEARCHED.stream()
                        .map(field -> cb.like(cb.lower(root.<String>get(field)), like, '\\'))
                        .toList());
                anyOf.add(cb.like(cb.lower(root.join("participantType", JoinType.LEFT).<String>get("name")), like, '\\'));
                where.add(cb.or(anyOf.toArray(Predicate[]::new)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /** {@code page} is 0-based; out-of-range values are pulled back in (see Paging). */
    public static Pageable pageRequest(int page, int size) {
        return Paging.of(page, size, NEWEST_FIRST);
    }

    // So a search for "50%" or "a_b" matches those characters literally.
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
