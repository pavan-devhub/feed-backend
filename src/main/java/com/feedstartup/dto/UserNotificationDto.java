package com.feedstartup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One entry in a user's notifications. The frontend words it (utils/notifications.js), so this
 * carries the facts only:
 * <ul>
 *   <li>{@code registered} / {@code volunteered} - their own sign-up, with {@code epm}</li>
 *   <li>{@code epm-update} - the admin changed or cancelled an EPM they signed up for: {@code epm} + {@code update}</li>
 *   <li>{@code epm-reminder} - an EPM they signed up for is {@code daysLeft} days away (7, 14... or 1): {@code epm}</li>
 *   <li>{@code new-epm} - an EPM is open for registration and volunteering, announced 15 days ahead: {@code epm}</li>
 *   <li>{@code new-publication} - a Feed World issue became available: {@code publication}</li>
 * </ul>
 * {@code id} is stable across requests (e.g. "update-42"), {@code createdAt} is when it happened.
 */
public record UserNotificationDto(
        String id,
        String type,
        LocalDateTime createdAt,
        boolean unread,
        Epm epm,
        EpmEventUpdateDto update,
        Publication publication,
        Integer daysLeft) {

    public record Epm(Long id, String title, String city, String venue, LocalDate eventDate, String timeRange, boolean cancelled) {}

    /** {@code id} is the reader's issue id, e.g. "2026-10-English". */
    public record Publication(String id, int year, int month, String language) {}

    public UserNotificationDto withUnread(boolean unread) {
        return new UserNotificationDto(id, type, createdAt, unread, epm, update, publication, daysLeft);
    }
}
