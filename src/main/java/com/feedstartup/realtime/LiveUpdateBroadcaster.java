package com.feedstartup.realtime;

import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.security.StompPrincipal;
import com.feedstartup.service.EpmSignUpOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sends the STOMP messages (see LiveUpdate) for the changes the services announce, plus the two
 * that no one's action triggers: midnight, when EPMs move from upcoming to previous and the
 * 15-day announcements and countdown reminders come due (EpmNotificationSchedule), and the 1st of
 * the month, when that month's Feed World issue comes out (PublicationVisibility).
 */
@Component
public class LiveUpdateBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdateBroadcaster.class);

    private final SimpMessagingTemplate messaging;
    private final EpmRegistrationRepository registrationRepository;
    private final EpmVolunteerRepository volunteerRepository;
    private final EpmSignUpOwner signUpOwner;

    public LiveUpdateBroadcaster(SimpMessagingTemplate messaging,
                                 EpmRegistrationRepository registrationRepository,
                                 EpmVolunteerRepository volunteerRepository,
                                 EpmSignUpOwner signUpOwner) {
        this.messaging = messaging;
        this.registrationRepository = registrationRepository;
        this.volunteerRepository = volunteerRepository;
        this.signUpOwner = signUpOwner;
    }

    // Each listener runs after its transaction commits, so a browser reloading on the message
    // already finds the change; fallbackExecution covers callers that don't run in a transaction.

    @TransactionalEventListener(fallbackExecution = true)
    public void onEpmChanged(LiveUpdateEvents.EpmChanged e) {
        broadcast(LiveUpdate.EPM_EVENTS_TOPIC, new LiveUpdate(LiveUpdate.EPM_CHANGED, e.eventId()));
        // Everyone signed up for it gets the change on their bell and Status of Activities.
        for (Long userId : signedUpUsers(e.eventId())) {
            sendToUser(userId, new LiveUpdate(LiveUpdate.MY_EPM_CHANGED, e.eventId()));
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onEpmSignedUp(LiveUpdateEvents.EpmSignedUp e) {
        broadcast(LiveUpdate.EPM_EVENTS_TOPIC, new LiveUpdate(LiveUpdate.SIGNUPS_CHANGED, e.eventId()));
        // Their other tabs and devices pick up the new sign-up too.
        if (e.userId() != null) {
            sendToUser(e.userId(), new LiveUpdate(LiveUpdate.MY_EPM_CHANGED, e.eventId()));
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onPublicationsChanged(LiveUpdateEvents.PublicationsChanged e) {
        broadcast(LiveUpdate.PUBLICATIONS_TOPIC, new LiveUpdate(LiveUpdate.PUBLICATIONS_CHANGED, null));
    }

    /** Just after midnight in the server's time zone - the one "upcoming" and the EPM notifications are reckoned in. */
    @Scheduled(cron = "5 0 0 * * *")
    public void dayChanged() {
        broadcast(LiveUpdate.EPM_EVENTS_TOPIC, new LiveUpdate(LiveUpdate.DAY_CHANGED, null));
    }

    /** Just after midnight on the 1st, in the zone Feed World issues are released in. */
    @Scheduled(cron = "5 0 0 1 * *", zone = "${feedworld.publications.release-zone:Asia/Kolkata}")
    public void issuesReleased() {
        broadcast(LiveUpdate.PUBLICATIONS_TOPIC, new LiveUpdate(LiveUpdate.PUBLICATIONS_CHANGED, null));
    }

    /** The accounts that registered or volunteered for this EPM, matched the way Status of Activities matches them. */
    private Set<Long> signedUpUsers(Long eventId) {
        Set<Long> userIds = new LinkedHashSet<>();
        registrationRepository.findByEpmEventId(eventId).forEach(r ->
                addIfKnown(userIds, signUpOwner.resolve(r.getUserId(), r.getEmail(), r.getMobileNumber())));
        volunteerRepository.findByEpmEventId(eventId).forEach(v ->
                addIfKnown(userIds, signUpOwner.resolve(v.getUserId(), v.getEmail(), v.getMobileNumber())));
        return userIds;
    }

    private static void addIfKnown(Set<Long> userIds, Long userId) {
        if (userId != null) userIds.add(userId);
    }

    // A failed send must never fail the request that made the change - it is already saved, and
    // the pages still pick it up when they next load or reconnect.

    private void broadcast(String destination, LiveUpdate update) {
        try {
            messaging.convertAndSend(destination, update);
        } catch (RuntimeException ex) {
            log.warn("Live update {} to {} failed", update.type(), destination, ex);
        }
    }

    private void sendToUser(Long userId, LiveUpdate update) {
        try {
            messaging.convertAndSendToUser(StompPrincipal.forUser(userId).getName(), LiveUpdate.MY_UPDATES_QUEUE, update);
        } catch (RuntimeException ex) {
            log.warn("Live update {} to user {} failed", update.type(), userId, ex);
        }
    }
}
