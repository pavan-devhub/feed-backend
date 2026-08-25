package com.feedstartup.service;

import com.feedstartup.dto.ActiveSessionDto;
import com.feedstartup.model.User;
import com.feedstartup.model.UserSession;

import java.util.List;

public interface SessionService {

    UserSession createSession(User user, String userAgent, String ipAddress);

    List<ActiveSessionDto> listSessions(Long userId, String currentJti);

    void revokeSession(Long userId, Long sessionId);

    boolean isActive(String jti);

    void touch(String jti);

    void deleteByJti(String jti);
}
