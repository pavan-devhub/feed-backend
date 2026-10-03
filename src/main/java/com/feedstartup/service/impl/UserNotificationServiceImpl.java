package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmEventUpdateDto;
import com.feedstartup.dto.UserNotificationDto;
import com.feedstartup.dto.UserNotificationsDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmRegistration;
import com.feedstartup.model.EpmVolunteer;
import com.feedstartup.model.Publication;
import com.feedstartup.model.User;
import com.feedstartup.model.UserNotificationState;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.repository.PublicationRepository;
import com.feedstartup.repository.UserNotificationStateRepository;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.EpmNotificationSchedule;
import com.feedstartup.service.PublicationService;
import com.feedstartup.service.PublicationVisibility;
import com.feedstartup.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Notifications are worked out on each request from data that already exists - the user's EPM
 * sign-ups, the EPM update log, the EPMs coming up and the Feed World issues they can read -
 * rather than stored one by one; UserNotificationState only remembers when they last looked.
 * Announcements and countdown reminders arrive on the dates EpmNotificationSchedule gives them.
 */
@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    /** At most this many notifications, newest first. */
    static final int LIMIT = 30;

    private static final Comparator<UserNotificationDto> NEWEST_FIRST =
            Comparator.comparing(UserNotificationDto::createdAt).reversed();

    private final UserRepository userRepository;
    private final EpmRegistrationRepository registrationRepository;
    private final EpmVolunteerRepository volunteerRepository;
    private final EpmEventRepository eventRepository;
    private final EpmEventUpdateRepository updateRepository;
    private final PublicationRepository publicationRepository;
    private final PublicationService publicationService;
    private final PublicationVisibility publicationVisibility;
    private final UserNotificationStateRepository stateRepository;
    private final Clock clock;

    @Autowired
    public UserNotificationServiceImpl(UserRepository userRepository,
                                       EpmRegistrationRepository registrationRepository,
                                       EpmVolunteerRepository volunteerRepository,
                                       EpmEventRepository eventRepository,
                                       EpmEventUpdateRepository updateRepository,
                                       PublicationRepository publicationRepository,
                                       PublicationService publicationService,
                                       PublicationVisibility publicationVisibility,
                                       UserNotificationStateRepository stateRepository) {
        this(userRepository, registrationRepository, volunteerRepository, eventRepository, updateRepository,
                publicationRepository, publicationService, publicationVisibility, stateRepository, Clock.systemDefaultZone());
    }

    /** Pins "now" - for tests. */
    UserNotificationServiceImpl(UserRepository userRepository,
                                EpmRegistrationRepository registrationRepository,
                                EpmVolunteerRepository volunteerRepository,
                                EpmEventRepository eventRepository,
                                EpmEventUpdateRepository updateRepository,
                                PublicationRepository publicationRepository,
                                PublicationService publicationService,
                                PublicationVisibility publicationVisibility,
                                UserNotificationStateRepository stateRepository,
                                Clock clock) {
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.volunteerRepository = volunteerRepository;
        this.eventRepository = eventRepository;
        this.updateRepository = updateRepository;
        this.publicationRepository = publicationRepository;
        this.publicationService = publicationService;
        this.publicationVisibility = publicationVisibility;
        this.stateRepository = stateRepository;
        this.clock = clock;
    }

    @Override
    public UserNotificationsDto forUser(String email) {
        User user = findUser(email);
        LocalDate today = LocalDate.now(clock);
        List<UserNotificationDto> all = new ArrayList<>();

        List<EpmRegistration> registrations = registrationRepository.findForUser(user.getId(), user.getEmail(), user.getPhone());
        List<EpmVolunteer> volunteers = volunteerRepository.findForUser(user.getId(), user.getEmail(), user.getPhone());
        Map<Long, LocalDateTime> signedUpAt = new HashMap<>();
        registrations.forEach(r -> signedUpAt.merge(r.getEpmEventId(), r.getCreatedAt(), UserNotificationServiceImpl::earlier));
        volunteers.forEach(v -> signedUpAt.merge(v.getEpmEventId(), v.getCreatedAt(), UserNotificationServiceImpl::earlier));

        // Like "Status of Activities", only EPMs still to come - news about past ones is stale.
        Map<Long, EpmEvent> upcoming = eventRepository.findAllById(signedUpAt.keySet()).stream()
                .filter(e -> !e.getEventDate().isBefore(today))
                .collect(Collectors.toMap(EpmEvent::getId, Function.identity()));

        // Their own sign-ups - there the moment the form is sent.
        for (EpmRegistration r : registrations) {
            EpmEvent e = upcoming.get(r.getEpmEventId());
            if (e != null) all.add(epmNotification("registered-" + r.getId(), "registered", r.getCreatedAt(), e, null, null));
        }
        for (EpmVolunteer v : volunteers) {
            EpmEvent e = upcoming.get(v.getEpmEventId());
            if (e != null) all.add(epmNotification("volunteered-" + v.getId(), "volunteered", v.getCreatedAt(), e, null, null));
        }

        // What the admin changed since: rescheduled, moved, new time, cancelled, reinstated...
        if (!upcoming.isEmpty()) {
            for (EpmEventUpdate u : updateRepository.findByEpmEventIdInOrderByCreatedAtAscIdAsc(upcoming.keySet())) {
                // A change made before they signed up was already part of what they signed up for.
                if (u.getCreatedAt().isAfter(signedUpAt.get(u.getEpmEventId()))) {
                    all.add(epmNotification("update-" + u.getId(), "epm-update", u.getCreatedAt(),
                            upcoming.get(u.getEpmEventId()), EpmEventUpdateDto.from(u), null));
                }
            }
        }

        // The countdown: every 7 days before each EPM they signed up for, and the day before. One per
        // EPM even if they both registered and volunteered, and only the latest - it supersedes the rest.
        for (EpmEvent e : upcoming.values()) {
            if (e.isCancelled()) continue;
            EpmNotificationSchedule.latestReminder(e.getEventDate(), today, signedUpAt.get(e.getId()))
                    .ifPresent(r -> all.add(epmNotification("reminder-" + e.getId() + "-" + r.daysLeft(), "epm-reminder",
                            r.at(), e, null, r.daysLeft())));
        }

        // EPMs being announced to everyone - except the ones they've already signed up for.
        for (EpmEvent e : announcedEpms(today)) {
            if (!signedUpAt.containsKey(e.getId())) all.add(announcement(e));
        }

        // An issue is announced only once its PDF is on the server - a row whose file is gone
        // would open as "not available" - and only from the 1st of its month (see
        // PublicationVisibility), for admins too. One uploaded ahead, e.g. October's on 30
        // September, appears on 1 October dated 1 October; one uploaded after the 1st is dated
        // when it was uploaded. This month's and last month's issues are announced.
        YearMonth latest = publicationVisibility.latestReleasedMonth();
        for (YearMonth month : List.of(latest, latest.minusMonths(1))) {
            LocalDateTime releasedAt = month.atDay(1).atStartOfDay();
            for (Publication p : publicationRepository.findByYearAndMonth(month.getYear(), month.getMonthValue(), PublicationRepository.CATALOG_ORDER)) {
                if (!publicationService.pdfExists(p)) continue;
                String id = PublicationServiceImpl.idOf(p.getYear(), p.getMonth(), p.getLanguage());
                LocalDateTime availableAt = p.getCreatedAt() != null && p.getCreatedAt().isAfter(releasedAt) ? p.getCreatedAt() : releasedAt;
                all.add(new UserNotificationDto("publication-" + id, "new-publication", availableAt, false, null, null,
                        new UserNotificationDto.Publication(id, p.getYear(), p.getMonth(), p.getLanguage().name()), null));
            }
        }

        LocalDateTime seenAt = stateRepository.findById(user.getId()).map(UserNotificationState::getSeenAt).orElse(null);
        List<UserNotificationDto> items = all.stream()
                .sorted(NEWEST_FIRST)
                .limit(LIMIT)
                .map(n -> n.withUnread(seenAt == null || n.createdAt().isAfter(seenAt)))
                .toList();
        return new UserNotificationsDto(items, items.stream().filter(UserNotificationDto::unread).count());
    }

    @Override
    @Transactional
    public void markAllSeen(String email) {
        User user = findUser(email);
        stateRepository.save(new UserNotificationState(user.getId(), LocalDateTime.now(clock)));
    }

    @Override
    public UserNotificationsDto announcements() {
        List<UserNotificationDto> items = announcedEpms(LocalDate.now(clock)).stream()
                .map(UserNotificationServiceImpl::announcement)
                .sorted(NEWEST_FIRST)
                .limit(LIMIT)
                .toList();
        return new UserNotificationsDto(items, 0);
    }

    /**
     * Upcoming EPMs that are being announced: held between today and 15 days from now, whenever
     * the admin added them - one added months ahead is announced 15 days before, not on the day.
     */
    private List<EpmEvent> announcedEpms(LocalDate today) {
        return eventRepository.findByCancelledFalseAndEventDateBetween(today, today.plusDays(EpmNotificationSchedule.ANNOUNCE_DAYS_BEFORE));
    }

    private static UserNotificationDto announcement(EpmEvent e) {
        return epmNotification("epm-" + e.getId(), "new-epm", EpmNotificationSchedule.announcedAt(e), e, null, null);
    }

    private static UserNotificationDto epmNotification(String id, String type, LocalDateTime at, EpmEvent e,
                                                       EpmEventUpdateDto update, Integer daysLeft) {
        UserNotificationDto.Epm epm = new UserNotificationDto.Epm(e.getId(), e.getTitle(), e.getCity(), e.getVenue(),
                e.getEventDate(), e.getTimeRange(), e.isCancelled());
        return new UserNotificationDto(id, type, at, false, epm, update, null, daysLeft);
    }

    private static LocalDateTime earlier(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
