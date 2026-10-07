package com.feedstartup.realtime;

/**
 * One STOMP message to the browser, e.g. {"type":"EPM_CHANGED","eventId":199}. It only says what
 * changed, never the data itself: the page reloads that through the REST API it already uses, so
 * access checks stay in one place, and a message missed while disconnected costs nothing (the
 * browser reloads everything it shows when it reconnects).
 */
public record LiveUpdate(String type, Long eventId) {

    /** Every EPM list, and every bell (newly announced EPMs). Public - visitors who aren't logged in too. */
    public static final String EPM_EVENTS_TOPIC = "/topic/epm-events";

    /** Feed World issues, for the bells of logged-in users. */
    public static final String PUBLICATIONS_TOPIC = "/topic/publications";

    /**
     * One person's own sign-ups. Sent with convertAndSendToUser, so the browser subscribes to
     * "/user/queue/updates" and Spring routes it to that user's connections only.
     */
    public static final String MY_UPDATES_QUEUE = "/queue/updates";

    /** An EPM was added, edited, cancelled, reinstated or deleted. */
    public static final String EPM_CHANGED = "EPM_CHANGED";

    /** Someone registered or volunteered for an EPM - only the sign-up counts on the lists move. */
    public static final String SIGNUPS_CHANGED = "SIGNUPS_CHANGED";

    /** Midnight: EPMs move from upcoming to previous, and the date-based notifications come due. */
    public static final String DAY_CHANGED = "DAY_CHANGED";

    /** A Feed World issue was uploaded or removed, or came out on the 1st of its month. */
    public static final String PUBLICATIONS_CHANGED = "PUBLICATIONS_CHANGED";

    /** Something on this person's own bell / Status of Activities changed. */
    public static final String MY_EPM_CHANGED = "MY_EPM_CHANGED";
}
