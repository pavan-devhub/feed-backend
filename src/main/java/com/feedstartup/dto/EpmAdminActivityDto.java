package com.feedstartup.dto;

import com.feedstartup.model.EpmAdminActivity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * One row of the admin panel's EPM Activity log. {@code action} and {@code field} are the
 * lower-case EpmAdminActivity enum names, e.g. "updated" / "venue"; {@code field} is null unless
 * the action is "updated". The event details are as they stood right after the action.
 */
public record EpmAdminActivityDto(Long id, LocalDateTime createdAt, Long adminId, String adminUsername,
                                  String action, String field, Long epmEventId, String eventTitle,
                                  LocalDate eventDate, String eventCity, String oldValue, String newValue) {

    public static EpmAdminActivityDto from(EpmAdminActivity a) {
        return new EpmAdminActivityDto(a.getId(), a.getCreatedAt(), a.getAdminId(), a.getAdminUsername(),
                a.getAction().name().toLowerCase(Locale.ROOT),
                a.getField() == null ? null : a.getField().name().toLowerCase(Locale.ROOT),
                a.getEpmEventId(), a.getEventTitle(), a.getEventDate(), a.getEventCity(), a.getOldValue(), a.getNewValue());
    }

    /** An admin the log's "Admin" filter can pick. */
    public record Admin(Long id, String username) {}
}
