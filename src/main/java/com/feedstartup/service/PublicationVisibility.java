package com.feedstartup.service;

import com.feedstartup.model.Publication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.function.Predicate;

/**
 * Which Feed World issues the current caller may see. An issue's month is its release date: a
 * normal user sees it from the 1st of that month onwards - the current month and every month
 * before it - and never a later month's issue, even if the admin has already uploaded it. So on
 * 29 or 30 September the newest visible issue is September's, and October's appears by itself
 * on 1 October; a future year shows up only once its January arrives. Admins (ROLE_ADMIN) see
 * every issue, so they can prepare months ahead of release.
 *
 * "Today" is read in {@code feedworld.publications.release-zone} rather than the server's own
 * zone, so a month goes live at midnight on the 1st in India wherever the server happens to run.
 */
@Component
public class PublicationVisibility {

    private final Clock clock;

    @Autowired
    public PublicationVisibility(@Value("${feedworld.publications.release-zone:Asia/Kolkata}") String releaseZone) {
        this(Clock.system(ZoneId.of(releaseZone)));
    }

    /** Pins "today" - for tests. */
    public PublicationVisibility(Clock clock) {
        this.clock = clock;
    }

    /** The newest month a normal user can see: the current month in the release zone. */
    public YearMonth latestReleasedMonth() {
        return YearMonth.now(clock);
    }

    /**
     * A filter for the current caller. "Now" is read once, when the filter is made, so a single
     * response never straddles a month boundary.
     */
    public Predicate<Publication> viewableByCaller() {
        if (callerIsAdmin()) {
            return publication -> true;
        }
        YearMonth latest = latestReleasedMonth();
        return publication -> !YearMonth.of(publication.getYear(), publication.getMonth()).isAfter(latest);
    }

    public boolean canView(Publication publication) {
        return viewableByCaller().test(publication);
    }

    private static boolean callerIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
