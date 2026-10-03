package com.feedstartup.service.impl;

import com.feedstartup.dto.UserNotificationDto;
import com.feedstartup.dto.UserNotificationsDto;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmEventUpdate.Field;
import com.feedstartup.model.EpmRegistration;
import com.feedstartup.model.EpmVolunteer;
import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.model.User;
import com.feedstartup.model.UserNotificationState;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.repository.PublicationRepository;
import com.feedstartup.repository.UserNotificationStateRepository;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.PublicationService;
import com.feedstartup.service.PublicationVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** What lands in a user's notifications, in which order, and which count as new. */
class UserNotificationServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EpmRegistrationRepository registrationRepository = mock(EpmRegistrationRepository.class);
    private final EpmVolunteerRepository volunteerRepository = mock(EpmVolunteerRepository.class);
    private final EpmEventRepository eventRepository = mock(EpmEventRepository.class);
    private final EpmEventUpdateRepository updateRepository = mock(EpmEventUpdateRepository.class);
    private final PublicationRepository publicationRepository = mock(PublicationRepository.class);
    private final PublicationService publicationService = mock(PublicationService.class);
    private final UserNotificationStateRepository stateRepository = mock(UserNotificationStateRepository.class);
    private final UserNotificationServiceImpl service = serviceAt(Clock.systemDefaultZone());

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDate today = LocalDate.now();
    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(5L);
        user.setEmail("ravi@example.com");
        user.setPhone("9876543210");
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(user));
        when(publicationRepository.findByYearAndMonth(anyInt(), anyInt(), any())).thenReturn(List.of());
        when(publicationService.pdfExists(any())).thenReturn(true);
    }

    @Test
    void collectsSignUpsLaterUpdatesRemindersNewEpmsAndIssuesNewestFirst() {
        EpmEvent upcoming = event(11L, today.plusDays(10), now.minusDays(20));
        EpmEvent past = event(12L, today.minusDays(2), now.minusDays(40));
        EpmEvent opened = event(13L, today.plusDays(12), now.minusHours(3));   // added inside the 15-day window
        EpmEvent addedEarly = event(14L, today.plusDays(9), now.minusDays(60)); // announced 15 days before, not when added
        when(registrationRepository.findForUser(5L, "ravi@example.com", "9876543210")).thenReturn(List.of(
                registration(1L, 11L, now.minusDays(5)),
                registration(2L, 12L, now.minusDays(30))));
        when(volunteerRepository.findForUser(5L, "ravi@example.com", "9876543210")).thenReturn(List.of(
                volunteer(7L, 11L, now.minusDays(4))));
        when(eventRepository.findAllById(any())).thenReturn(List.of(upcoming, past));
        when(updateRepository.findByEpmEventIdInOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of(
                update(100L, 11L, Field.VENUE, now.minusDays(6)),   // before they signed up - not news
                update(101L, 11L, Field.TIME, now.minusHours(1))));
        when(eventRepository.findByCancelledFalseAndEventDateBetween(today, today.plusDays(15)))
                .thenReturn(List.of(upcoming, opened, addedEarly)); // they're already signed up for 11
        YearMonth thisMonth = YearMonth.now();
        Publication issue = issue(thisMonth, PublicationLanguage.English,
                thisMonth.atDay(1).atStartOfDay().minusDays(3)); // uploaded ahead of release
        when(publicationRepository.findByYearAndMonth(eq(thisMonth.getYear()), eq(thisMonth.getMonthValue()), any()))
                .thenReturn(List.of(issue));
        when(stateRepository.findById(5L)).thenReturn(Optional.of(new UserNotificationState(5L, now.minusDays(2))));

        UserNotificationsDto result = service.forUser("ravi@example.com");

        List<String> ids = result.items().stream().map(UserNotificationDto::id).toList();
        String issueId = "publication-" + thisMonth.getYear() + "-" + String.format("%02d", thisMonth.getMonthValue()) + "-English";
        // 10 days to go: the "14 days to go" reminder went out 4 days ago, after they signed up.
        assertTrue(ids.containsAll(List.of("update-101", "epm-13", "volunteered-7", "reminder-11-14", "registered-1",
                "epm-14", issueId)), ids.toString());
        assertEquals(7, ids.size(), ids.toString()); // no past EPM, no pre-sign-up update, no "new" EPM they joined
        assertEquals("update-101", ids.get(0));
        assertEquals("epm-13", ids.get(1));
        UserNotificationDto issueNote = byId(result, issueId);
        assertEquals(thisMonth.atDay(1).atStartOfDay(), issueNote.createdAt()); // announced on release, not upload
        assertEquals(today.minusDays(6).atStartOfDay(), byId(result, "epm-14").createdAt());
        UserNotificationDto reminder = byId(result, "reminder-11-14");
        assertEquals("epm-reminder", reminder.type());
        assertEquals(14, reminder.daysLeft());
        assertEquals(today.minusDays(4).atStartOfDay(), reminder.createdAt());
        assertEquals("time", result.items().get(0).update().field());
        // Seen two days ago: the update and the new EPM are new, the sign-ups (4-5 days old) aren't.
        assertTrue(result.items().get(0).unread());
        assertEquals(result.items().stream().filter(UserNotificationDto::unread).count(), result.unreadCount());
        assertTrue(result.items().stream().filter(n -> n.id().startsWith("registered") || n.id().startsWith("volunteered"))
                .noneMatch(UserNotificationDto::unread));
    }

    @Test
    void remindsEverySevenDaysThenTheDayBeforeButNotForACancelledEpm() {
        EpmEvent soon = event(21L, LocalDate.of(2026, 10, 20), LocalDateTime.of(2026, 9, 1, 9, 0));
        EpmEvent cancelled = event(22L, LocalDate.of(2026, 10, 20), LocalDateTime.of(2026, 9, 1, 9, 0));
        cancelled.setCancelled(true);
        when(registrationRepository.findForUser(5L, "ravi@example.com", "9876543210")).thenReturn(List.of(
                registration(3L, 21L, LocalDateTime.of(2026, 9, 25, 18, 0)),
                registration(4L, 22L, LocalDateTime.of(2026, 9, 25, 18, 0))));
        when(eventRepository.findAllById(any())).thenReturn(List.of(soon, cancelled));
        when(stateRepository.findById(5L)).thenReturn(Optional.of(new UserNotificationState(5L, LocalDateTime.of(2026, 10, 12, 20, 0))));

        // 13 Oct: 7 days to go - out at midnight, so new since they looked on the 12th.
        UserNotificationDto week = reminderIn(serviceAt(indiaTime(2026, 10, 13, 9, 0)).forUser("ravi@example.com"));
        assertEquals("reminder-21-7", week.id());
        assertEquals(LocalDateTime.of(2026, 10, 13, 0, 0), week.createdAt());
        assertTrue(week.unread());

        // 16 Oct: still the 7-day one; 19 Oct: the day-before reminder replaces it.
        assertEquals("reminder-21-7", reminderIn(serviceAt(indiaTime(2026, 10, 16, 9, 0)).forUser("ravi@example.com")).id());
        UserNotificationDto tomorrow = reminderIn(serviceAt(indiaTime(2026, 10, 19, 9, 0)).forUser("ravi@example.com"));
        assertEquals("reminder-21-1", tomorrow.id());
        assertEquals(1, tomorrow.daysLeft());
    }

    @Test
    void visitorsWhoArentLoggedInSeeOnlyTheEpmsBeingAnnounced() {
        EpmEvent addedEarly = event(31L, LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 8, 1, 9, 0));
        EpmEvent addedLate = event(32L, LocalDate.of(2026, 10, 10), LocalDateTime.of(2026, 10, 1, 16, 30));
        when(eventRepository.findByCancelledFalseAndEventDateBetween(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 17)))
                .thenReturn(List.of(addedEarly, addedLate));

        UserNotificationsDto result = serviceAt(indiaTime(2026, 10, 2, 11, 0)).announcements();

        assertEquals(List.of("epm-32", "epm-31"), result.items().stream().map(UserNotificationDto::id).toList());
        assertEquals(LocalDateTime.of(2026, 10, 1, 16, 30), result.items().get(0).createdAt());
        assertEquals(LocalDateTime.of(2026, 9, 30, 0, 0), result.items().get(1).createdAt()); // 15 days before 15 Oct
        assertTrue(result.items().stream().allMatch(n -> n.type().equals("new-epm") && !n.unread()));
        assertEquals(0, result.unreadCount());
    }

    @Test
    void anIssueUploadedEarlyIsAnnouncedFromTheFirstOfItsMonthAndOnlyWithItsPdf() {
        // October 2026 uploaded on 30 September; September's row has lost its PDF.
        Publication october = issue(YearMonth.of(2026, 10), PublicationLanguage.English, LocalDateTime.of(2026, 9, 30, 10, 0));
        Publication september = issue(YearMonth.of(2026, 9), PublicationLanguage.English, LocalDateTime.of(2026, 9, 1, 9, 0));
        when(publicationRepository.findByYearAndMonth(eq(2026), eq(10), any())).thenReturn(List.of(october));
        when(publicationRepository.findByYearAndMonth(eq(2026), eq(9), any())).thenReturn(List.of(september));
        when(publicationService.pdfExists(september)).thenReturn(false);
        when(stateRepository.findById(5L)).thenReturn(Optional.of(new UserNotificationState(5L, LocalDateTime.of(2026, 9, 30, 23, 0))));

        // 30 September, late evening in India: October isn't out yet, September can't be read.
        assertTrue(serviceAt(indiaTime(2026, 9, 30, 23, 30)).forUser("ravi@example.com").items().isEmpty());

        // Just after midnight on 1 October: October is announced - dated the 1st, not the upload -
        // and is new, since they last looked on 30 September.
        UserNotificationsDto result = serviceAt(indiaTime(2026, 10, 1, 0, 5)).forUser("ravi@example.com");
        assertEquals(List.of("publication-2026-10-English"), result.items().stream().map(UserNotificationDto::id).toList());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), result.items().get(0).createdAt());
        assertTrue(result.items().get(0).unread());
        assertFalse(result.items().stream().anyMatch(n -> n.id().contains("2026-09")));
    }

    @Test
    void markingSeenStoresTheTime() {
        service.markAllSeen("ravi@example.com");
        ArgumentCaptor<UserNotificationState> saved = ArgumentCaptor.forClass(UserNotificationState.class);
        verify(stateRepository).save(saved.capture());
        assertEquals(5L, saved.getValue().getUserId());
        assertTrue(!saved.getValue().getSeenAt().isBefore(now));
    }

    private UserNotificationServiceImpl serviceAt(Clock clock) {
        return new UserNotificationServiceImpl(userRepository, registrationRepository, volunteerRepository, eventRepository,
                updateRepository, publicationRepository, publicationService, new PublicationVisibility(clock), stateRepository, clock);
    }

    private static UserNotificationDto byId(UserNotificationsDto result, String id) {
        return result.items().stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow(() -> new AssertionError(id));
    }

    /** The single countdown reminder in {@code result}. */
    private static UserNotificationDto reminderIn(UserNotificationsDto result) {
        List<UserNotificationDto> reminders = result.items().stream().filter(n -> n.type().equals("epm-reminder")).toList();
        assertEquals(1, reminders.size(), reminders.toString());
        return reminders.get(0);
    }

    private static Clock indiaTime(int year, int month, int day, int hour, int minute) {
        ZoneId india = ZoneId.of("Asia/Kolkata");
        return Clock.fixed(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, india).toInstant(), india);
    }

    private static Publication issue(YearMonth month, PublicationLanguage language, LocalDateTime uploadedAt) {
        Publication p = new Publication();
        p.setYear(month.getYear());
        p.setMonth(month.getMonthValue());
        p.setLanguage(language);
        p.setCreatedAt(uploadedAt);
        return p;
    }

    private static EpmEvent event(Long id, LocalDate date, LocalDateTime createdAt) {
        EpmEvent e = new EpmEvent();
        e.setId(id);
        e.setTitle("EPM " + id);
        e.setCity("Vijayawada");
        e.setVenue("Town Hall");
        e.setEventDate(date);
        e.setCreatedAt(createdAt);
        return e;
    }

    private static EpmRegistration registration(Long id, Long eventId, LocalDateTime at) {
        EpmRegistration r = new EpmRegistration();
        r.setId(id);
        r.setEpmEventId(eventId);
        r.setCreatedAt(at);
        return r;
    }

    private static EpmVolunteer volunteer(Long id, Long eventId, LocalDateTime at) {
        EpmVolunteer v = new EpmVolunteer();
        v.setId(id);
        v.setEpmEventId(eventId);
        v.setCreatedAt(at);
        return v;
    }

    private static EpmEventUpdate update(Long id, Long eventId, Field field, LocalDateTime at) {
        EpmEventUpdate u = new EpmEventUpdate(eventId, field, "old", "new");
        u.setId(id);
        u.setCreatedAt(at);
        return u;
    }
}
