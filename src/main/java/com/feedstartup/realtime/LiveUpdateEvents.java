package com.feedstartup.realtime;

/**
 * What the services announce (through Spring's ApplicationEventPublisher) when something a live
 * page shows has changed. LiveUpdateBroadcaster turns each one into STOMP messages once the change
 * is saved, so the services themselves know nothing about WebSockets.
 */
public final class LiveUpdateEvents {

    private LiveUpdateEvents() {}

    /** An EPM was added, edited, cancelled, reinstated or deleted. */
    public record EpmChanged(Long eventId) {}

    /** A registration or volunteer sign-up for {@code eventId}, owned by {@code userId} (null when no account matches it). */
    public record EpmSignedUp(Long eventId, Long userId) {}

    /** A Feed World issue was uploaded or removed. */
    public record PublicationsChanged() {}
}
