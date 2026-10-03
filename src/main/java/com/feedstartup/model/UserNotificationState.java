package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * When a user last opened their notifications. Notifications themselves aren't stored - they're
 * worked out from EPM sign-ups, EPM updates and Feed World issues (see UserNotificationServiceImpl)
 * - so this one timestamp is all it takes to tell which of them are new.
 */
@Entity
@Table(name = "user_notification_state")
public class UserNotificationState {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    public UserNotificationState() {}

    public UserNotificationState(Long userId, LocalDateTime seenAt) {
        this.userId = userId;
        this.seenAt = seenAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
}
