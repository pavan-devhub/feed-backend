package com.feedstartup.service;

import com.feedstartup.model.SystemAdmin;
import com.feedstartup.model.SystemAdminSession;

/** The devices admins are logged in on - SessionService's counterpart for system_admin_sessions. */
public interface SystemAdminSessionService {

    /** Starts a login (or refreshes the one this browser already has) with a fresh jti for its JWT. */
    SystemAdminSession createSession(SystemAdmin admin, String userAgent, String ipAddress);

    boolean isActive(String jti);

    /** Records that the session was just used (at most every few minutes). */
    void touch(String jti);

    void deleteByJti(String jti);

    /** Logs the admin out on every device. */
    void deleteAllForAdmin(Long adminId);
}
