package com.feedstartup.service;

import com.feedstartup.model.EpmEvent;
import com.feedstartup.service.EpmNotificationSchedule.Reminder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** When EPM announcements and countdown reminders go out. */
class EpmNotificationScheduleTest {

    private static final LocalDate EVENT = LocalDate.of(2026, 11, 30);
    private static final LocalDateTime SIGNED_UP = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Test
    void remindsEverySevenDaysBeforeTheEpmAndAgainTheDayBefore() {
        // 59 days to go: the 63-day reminder was before they signed up - nothing yet.
        assertTrue(reminderOn(2026, 10, 2).isEmpty());
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 10, 5), 56)), reminderOn(2026, 10, 5));
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 10, 5), 56)), reminderOn(2026, 10, 6)); // still the latest
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 11, 16), 14)), reminderOn(2026, 11, 16));
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 11, 23), 7)), reminderOn(2026, 11, 23));
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 11, 23), 7)), reminderOn(2026, 11, 28));
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 11, 29), 1)), reminderOn(2026, 11, 29));
        assertEquals(Optional.of(new Reminder(LocalDate.of(2026, 11, 29), 1)), reminderOn(2026, 11, 30)); // on the day
        assertTrue(reminderOn(2026, 12, 1).isEmpty()); // over
    }

    @Test
    void sendsNoReminderFromBeforeSomeoneSignedUp() {
        LocalDateTime signedUpThatMorning = LocalDateTime.of(2026, 11, 23, 9, 0);
        assertTrue(EpmNotificationSchedule.latestReminder(EVENT, LocalDate.of(2026, 11, 23), signedUpThatMorning).isEmpty());
        assertEquals(1, EpmNotificationSchedule.latestReminder(EVENT, LocalDate.of(2026, 11, 29), signedUpThatMorning)
                .orElseThrow().daysLeft());
    }

    @Test
    void announcesFifteenDaysBeforeOrWhenAddedIfThatWasLater() {
        assertEquals(LocalDateTime.of(2026, 11, 15, 0, 0), EpmNotificationSchedule.announcedAt(event(LocalDateTime.of(2026, 8, 1, 12, 0))));
        assertEquals(LocalDateTime.of(2026, 11, 20, 16, 45), EpmNotificationSchedule.announcedAt(event(LocalDateTime.of(2026, 11, 20, 16, 45))));
    }

    private static Optional<Reminder> reminderOn(int year, int month, int day) {
        return EpmNotificationSchedule.latestReminder(EVENT, LocalDate.of(year, month, day), SIGNED_UP);
    }

    private static EpmEvent event(LocalDateTime createdAt) {
        EpmEvent e = new EpmEvent();
        e.setEventDate(EVENT);
        e.setCreatedAt(createdAt);
        return e;
    }
}
