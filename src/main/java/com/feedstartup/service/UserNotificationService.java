package com.feedstartup.service;

import com.feedstartup.dto.UserNotificationsDto;

/** The navbar bell and the dashboard's "Latest Alerts & Announcements" for a logged-in user. */
public interface UserNotificationService {

    /** @param email the logged-in account's email (the JWT principal) */
    UserNotificationsDto forUser(String email);

    /** Marks everything up to now as seen, clearing the unread count. */
    void markAllSeen(String email);

    /**
     * What the bell shows a visitor who isn't logged in: the upcoming EPMs being announced (from
     * 15 days before each), newest first. Nothing is unread here - the browser remembers when the
     * visitor last looked.
     */
    UserNotificationsDto announcements();
}
