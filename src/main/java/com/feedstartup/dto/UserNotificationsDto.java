package com.feedstartup.dto;

import java.util.List;

/** A user's notifications, newest first, and how many arrived since they last opened them. */
public record UserNotificationsDto(List<UserNotificationDto> items, long unreadCount) {}
