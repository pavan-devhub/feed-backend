package com.feedstartup.service;

import com.feedstartup.model.EpmEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * When the EPM notifications that aren't triggered by anyone's action "arrive" - the new-EPM
 * announcement and the countdown reminders. Notifications are worked out on each request (see
 * UserNotificationServiceImpl), so these are just dates: a notification shows up once its time has
 * come, dated that time.
 */
public final class EpmNotificationSchedule {

    /** An EPM is announced to everyone - logged in or not - this many days before it is held. */
    public static final int ANNOUNCE_DAYS_BEFORE = 15;

    /** People signed up for an EPM are reminded every this many days before it ("14 days to go", "7 days to go")... */
    public static final int REMINDER_EVERY_DAYS = 7;

    /** ...and once more the day before. */
    public static final int LAST_REMINDER_DAYS_BEFORE = 1;

    private EpmNotificationSchedule() {}

    /** The first day an EPM held on {@code eventDate} is announced. */
    public static LocalDate announcedFrom(LocalDate eventDate) {
        return eventDate.minusDays(ANNOUNCE_DAYS_BEFORE);
    }

    /**
     * When {@code e}'s announcement went out: the start of the day 15 days before it, or - for an
     * EPM the admin added later than that - the moment it was added. An EPM added months ahead
     * therefore stays quiet until 15 days before.
     */
    public static LocalDateTime announcedAt(EpmEvent e) {
        LocalDateTime windowOpens = announcedFrom(e.getEventDate()).atStartOfDay();
        return e.getCreatedAt() != null && e.getCreatedAt().isAfter(windowOpens) ? e.getCreatedAt() : windowOpens;
    }

    /** A countdown reminder: sent at the start of {@code date}, {@code daysLeft} days before the EPM. */
    public record Reminder(LocalDate date, int daysLeft) {
        public LocalDateTime at() {
            return date.atStartOfDay();
        }
    }

    /**
     * The most recent countdown reminder that has come due by {@code today} for someone who signed
     * up at {@code signedUpAt} - reminders go out 7, 14, 21... days before the EPM and the day before
     * it. Only the latest is returned (it supersedes the earlier ones), and none from before they
     * signed up. Empty once the EPM has passed.
     */
    public static Optional<Reminder> latestReminder(LocalDate eventDate, LocalDate today, LocalDateTime signedUpAt) {
        long daysLeft = ChronoUnit.DAYS.between(today, eventDate);
        if (daysLeft < 0) return Optional.empty();
        // The smallest reminder offset that is >= daysLeft is the one most recently sent.
        int offset = daysLeft <= LAST_REMINDER_DAYS_BEFORE
                ? LAST_REMINDER_DAYS_BEFORE
                : (int) (Math.ceil(daysLeft / (double) REMINDER_EVERY_DAYS) * REMINDER_EVERY_DAYS);
        Reminder reminder = new Reminder(eventDate.minusDays(offset), offset);
        // Every earlier reminder is older still, so if this one predates the sign-up, so do they.
        if (signedUpAt != null && !reminder.at().isAfter(signedUpAt)) return Optional.empty();
        return Optional.of(reminder);
    }
}
