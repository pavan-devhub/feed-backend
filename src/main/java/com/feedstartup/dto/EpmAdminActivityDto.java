package com.feedstartup.dto;

import com.feedstartup.model.EpmAdminActivity;
import com.feedstartup.model.EpmEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * One row of the admin panel's EPM Activity log. {@code action} and {@code field} are the
 * lower-case EpmAdminActivity enum names, e.g. "updated" / "venue"; {@code field} is null unless
 * the action is "updated".
 *
 * <p>{@code eventTitle}, {@code eventDate} and {@code eventCity} are the EPM as it stands now - as
 * the EPM Events list shows it, so every entry of a rescheduled EPM carries its new date - or, once
 * it is deleted, as it stood when it was deleted. {@code eventStatus} is "upcoming" (today or
 * later) or "previous" by that date, like the EPM Events tabs, or "deleted"; {@code eventCancelled}
 * is whether the EPM is cancelled now. {@code dateAtAction} is the EPM's date right after this
 * action - e.g. the date a "created" entry scheduled it for.
 */
public record EpmAdminActivityDto(Long id, LocalDateTime createdAt, Long adminId, String adminUsername,
                                  String action, String field, Long epmEventId, String eventTitle,
                                  LocalDate eventDate, String eventCity, String eventStatus, boolean eventCancelled,
                                  LocalDate dateAtAction, String oldValue, String newValue) {

    /**
     * @param current  the EPM as it stands now, or null if it has been deleted
     * @param deletion a deleted EPM's DELETED entry - how it last stood (null falls back to {@code a})
     * @param today    decides whether the EPM is upcoming or previous
     */
    public static EpmAdminActivityDto from(EpmAdminActivity a, EpmEvent current, EpmAdminActivity deletion, LocalDate today) {
        String title, city, status;
        LocalDate date;
        if (current != null) {
            title = current.getTitle();
            date = current.getEventDate();
            city = current.getCity();
            status = date.isBefore(today) ? "previous" : "upcoming";
        } else {
            EpmAdminActivity last = deletion != null ? deletion : a;
            title = last.getEventTitle();
            date = last.getEventDate();
            city = last.getEventCity();
            status = "deleted";
        }
        return new EpmAdminActivityDto(a.getId(), a.getCreatedAt(), a.getAdminId(), a.getAdminUsername(),
                a.getAction().name().toLowerCase(Locale.ROOT),
                a.getField() == null ? null : a.getField().name().toLowerCase(Locale.ROOT),
                a.getEpmEventId(), title, date, city, status, current != null && current.isCancelled(),
                a.getEventDate(), a.getOldValue(), a.getNewValue());
    }

    /** An admin the log's "Admin" filter can pick. */
    public record Admin(Long id, String username) {}
}
