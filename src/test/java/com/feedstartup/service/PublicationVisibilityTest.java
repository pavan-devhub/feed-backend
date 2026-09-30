package com.feedstartup.service;

import com.feedstartup.model.Publication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A month's issue is visible to normal users from the 1st of that month (India time); admins see all. */
class PublicationVisibilityTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @AfterEach
    void clearCaller() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onTheTwentyNinthOfSeptemberSeptemberIsTheNewestVisibleMonth() {
        PublicationVisibility visibility = at(LocalDateTime.of(2026, 9, 29, 12, 0));
        asUser();

        assertEquals(YearMonth.of(2026, 9), visibility.latestReleasedMonth());
        assertTrue(visibility.canView(issue(2026, 1)));
        assertTrue(visibility.canView(issue(2026, 9)));
        assertTrue(visibility.canView(issue(2025, 12)));
        assertFalse(visibility.canView(issue(2026, 10)));
        assertFalse(visibility.canView(issue(2026, 12)));
        assertFalse(visibility.canView(issue(2027, 1)));
        assertFalse(visibility.canView(issue(2029, 3)));
    }

    @Test
    void theLastMinuteOfSeptemberThirtiethStillEndsAtSeptember() {
        PublicationVisibility visibility = at(LocalDateTime.of(2026, 9, 30, 23, 59));
        asUser();

        assertTrue(visibility.canView(issue(2026, 9)));
        assertFalse(visibility.canView(issue(2026, 10)));
    }

    @Test
    void octoberOpensAtMidnightOnTheFirstInIndiaEvenWhileItIsStillSeptemberInUtc() {
        // 2026-10-01 00:00 IST is 2026-09-30 18:30 UTC.
        PublicationVisibility visibility = new PublicationVisibility(
                Clock.fixed(Instant.parse("2026-09-30T18:30:00Z"), INDIA));
        asUser();

        assertTrue(visibility.canView(issue(2026, 10)));
        assertFalse(visibility.canView(issue(2026, 11)));
    }

    @Test
    void aNewYearOnlyAppearsOnceItsJanuaryArrives() {
        asUser();
        assertFalse(at(LocalDateTime.of(2026, 12, 31, 23, 59)).canView(issue(2027, 1)));
        assertTrue(at(LocalDateTime.of(2027, 1, 1, 0, 0)).canView(issue(2027, 1)));
    }

    @Test
    void adminsSeeIssuesUploadedAheadOfRelease() {
        PublicationVisibility visibility = at(LocalDateTime.of(2026, 9, 29, 12, 0));
        asCaller("ROLE_ADMIN");

        assertTrue(visibility.canView(issue(2026, 10)));
        assertTrue(visibility.canView(issue(2029, 3)));
    }

    @Test
    void aCallerWithNoRoleIsTreatedAsANormalUser() {
        PublicationVisibility visibility = at(LocalDateTime.of(2026, 9, 29, 12, 0));

        assertTrue(visibility.canView(issue(2026, 9)));
        assertFalse(visibility.canView(issue(2026, 10)));
    }

    private static PublicationVisibility at(LocalDateTime indiaTime) {
        return new PublicationVisibility(Clock.fixed(indiaTime.atZone(INDIA).toInstant(), INDIA));
    }

    private static void asUser() {
        asCaller("ROLE_USER");
    }

    private static void asCaller(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "reader@feed.test", null, List.of(new SimpleGrantedAuthority(role))));
    }

    private static Publication issue(int year, int month) {
        Publication publication = new Publication();
        publication.setYear(year);
        publication.setMonth(month);
        return publication;
    }
}
