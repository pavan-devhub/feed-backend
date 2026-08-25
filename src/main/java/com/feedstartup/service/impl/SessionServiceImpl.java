package com.feedstartup.service.impl;

import com.feedstartup.dto.ActiveSessionDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.exception.TooManySessionsException;
import com.feedstartup.model.User;
import com.feedstartup.model.UserSession;
import com.feedstartup.repository.UserSessionRepository;
import com.feedstartup.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionService {

    private static final int MAX_SESSIONS_PER_USER = 3;
    private static final long TOUCH_THROTTLE_MINUTES = 5;

    private final UserSessionRepository userSessionRepository;

    // A session row is only ever proof of login for as long as its JWT is valid, so a row
    // older than the token lifetime is dead weight - treat it as expired and stop letting it
    // occupy one of the user's 3 device slots.
    @Value("${jwt.expiration:15552000000}")
    private long sessionTtlMillis;

    @Autowired
    public SessionServiceImpl(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    @Transactional
    public UserSession createSession(User user, String userAgent, String ipAddress) {
        purgeExpired(user.getId());

        // Re-logging in from the same browser/device shouldn't burn a second device slot -
        // refresh that device's existing row (and issue it a fresh jti) instead of duplicating it.
        UserSession existing = userSessionRepository
                .findByUserIdAndUserAgentAndIpAddress(user.getId(), userAgent, ipAddress)
                .orElse(null);
        if (existing != null) {
            existing.setJti(UUID.randomUUID().toString());
            existing.setLastSeenAt(LocalDateTime.now());
            return userSessionRepository.save(existing);
        }

        long activeCount = userSessionRepository.countByUserId(user.getId());
        if (activeCount >= MAX_SESSIONS_PER_USER) {
            List<ActiveSessionDto> sessions = listSessions(user.getId(), null);
            throw new TooManySessionsException(
                    "You are already logged in on " + MAX_SESSIONS_PER_USER + " devices. "
                            + "Log out from one of them to log in here.",
                    sessions);
        }

        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setJti(UUID.randomUUID().toString());
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        return userSessionRepository.save(session);
    }

    @Override
    public List<ActiveSessionDto> listSessions(Long userId, String currentJti) {
        purgeExpired(userId);

        return userSessionRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .sorted(Comparator.comparing(UserSession::getLastSeenAt).reversed())
                .map(session -> new ActiveSessionDto(
                        session.getId(),
                        deviceName(session.getUserAgent()),
                        browser(session.getUserAgent()),
                        os(session.getUserAgent()),
                        session.getIpAddress(),
                        session.getCreatedAt(),
                        session.getLastSeenAt(),
                        session.getJti().equals(currentJti)))
                .collect(Collectors.toList());
    }

    private void purgeExpired(Long userId) {
        LocalDateTime cutoff = LocalDateTime.now().minus(sessionTtlMillis, ChronoUnit.MILLIS);
        userSessionRepository.deleteByUserIdAndCreatedAtBefore(userId, cutoff);
    }

    @Override
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        userSessionRepository.delete(session);
    }

    @Override
    public boolean isActive(String jti) {
        return jti != null && userSessionRepository.findByJti(jti).isPresent();
    }

    @Override
    @Transactional
    public void touch(String jti) {
        if (jti == null) {
            return;
        }
        userSessionRepository.findByJti(jti).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            if (ChronoUnit.MINUTES.between(session.getLastSeenAt(), now) >= TOUCH_THROTTLE_MINUTES) {
                session.setLastSeenAt(now);
                userSessionRepository.save(session);
            }
        });
    }

    @Override
    @Transactional
    public void deleteByJti(String jti) {
        if (jti != null) {
            userSessionRepository.deleteByJti(jti);
        }
    }

    private String os(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown OS";
        }
        String ua = userAgent.toLowerCase();

        if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        } else if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac os")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        }
        return "Unknown OS";
    }

    private String browser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown browser";
        }
        String ua = userAgent.toLowerCase();

        if (ua.contains("edg/")) {
            return "Edge";
        } else if (ua.contains("chrome/") && !ua.contains("chromium")) {
            return "Chrome";
        } else if (ua.contains("firefox/")) {
            return "Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome")) {
            return "Safari";
        }
        return "Browser";
    }

    private String deviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        return browser(userAgent) + " on " + os(userAgent);
    }
}
