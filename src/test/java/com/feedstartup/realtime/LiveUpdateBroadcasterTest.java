package com.feedstartup.realtime;

import com.feedstartup.model.EpmRegistration;
import com.feedstartup.model.EpmVolunteer;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmSignUpOwner;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Which STOMP messages each change sends, and to whom. */
class LiveUpdateBroadcasterTest {

    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final EpmRegistrationRepository registrationRepository = mock(EpmRegistrationRepository.class);
    private final EpmVolunteerRepository volunteerRepository = mock(EpmVolunteerRepository.class);
    private final EpmSignUpOwner signUpOwner = mock(EpmSignUpOwner.class);
    private final LiveUpdateBroadcaster broadcaster =
            new LiveUpdateBroadcaster(messaging, registrationRepository, volunteerRepository, signUpOwner);

    @Test
    void anEpmChangeGoesToEveryListAndOnceToEachPersonSignedUpForIt() {
        // User 5 registered and volunteered; an older registration without a user id matches user 7
        // by email; one sign-up matches no account at all.
        when(registrationRepository.findByEpmEventId(199L)).thenReturn(List.of(
                registration(5L, null), registration(null, "asha@example.com"), registration(null, "nobody@example.com")));
        when(volunteerRepository.findByEpmEventId(199L)).thenReturn(List.of(volunteer(5L)));
        when(signUpOwner.resolve(5L, null, null)).thenReturn(5L);
        when(signUpOwner.resolve(null, "asha@example.com", null)).thenReturn(7L);
        when(signUpOwner.resolve(null, "nobody@example.com", null)).thenReturn(null);

        broadcaster.onEpmChanged(new LiveUpdateEvents.EpmChanged(199L));

        verify(messaging).convertAndSend("/topic/epm-events", new LiveUpdate("EPM_CHANGED", 199L));
        verify(messaging, times(1)).convertAndSendToUser("user-5", "/queue/updates", new LiveUpdate("MY_EPM_CHANGED", 199L));
        verify(messaging, times(1)).convertAndSendToUser("user-7", "/queue/updates", new LiveUpdate("MY_EPM_CHANGED", 199L));
        verify(messaging, times(2)).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void aSignUpUpdatesTheListCountsAndTheirOwnPagesOnly() {
        broadcaster.onEpmSignedUp(new LiveUpdateEvents.EpmSignedUp(199L, 5L));

        verify(messaging).convertAndSend("/topic/epm-events", new LiveUpdate("SIGNUPS_CHANGED", 199L));
        verify(messaging).convertAndSendToUser("user-5", "/queue/updates", new LiveUpdate("MY_EPM_CHANGED", 199L));
    }

    @Test
    void aSignUpWithNoAccountOnlyUpdatesTheListCounts() {
        broadcaster.onEpmSignedUp(new LiveUpdateEvents.EpmSignedUp(199L, null));

        verify(messaging).convertAndSend("/topic/epm-events", new LiveUpdate("SIGNUPS_CHANGED", 199L));
        verify(messaging, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void midnightAndTheMonthlyReleaseReachEveryone() {
        broadcaster.dayChanged();
        broadcaster.issuesReleased();
        broadcaster.onPublicationsChanged(new LiveUpdateEvents.PublicationsChanged());

        verify(messaging).convertAndSend("/topic/epm-events", new LiveUpdate("DAY_CHANGED", null));
        verify(messaging, times(2)).convertAndSend("/topic/publications", new LiveUpdate("PUBLICATIONS_CHANGED", null));
    }

    @Test
    void aFailedSendNeverFailsTheChangeThatTriggeredIt() {
        doThrow(new MessageDeliveryException("broker down"))
                .when(messaging).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> broadcaster.onEpmSignedUp(new LiveUpdateEvents.EpmSignedUp(199L, null)));
    }

    private static EpmRegistration registration(Long userId, String email) {
        EpmRegistration r = new EpmRegistration();
        r.setEpmEventId(199L);
        r.setUserId(userId);
        r.setEmail(email);
        return r;
    }

    private static EpmVolunteer volunteer(Long userId) {
        EpmVolunteer v = new EpmVolunteer();
        v.setEpmEventId(199L);
        v.setUserId(userId);
        return v;
    }
}
