package com.feedstartup.service.impl;

import com.feedstartup.model.SystemAdmin;
import com.feedstartup.model.SystemAdminSession;
import com.feedstartup.repository.SystemAdminSessionRepository;
import com.feedstartup.service.SystemAdminSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Like SessionServiceImpl, minus the 3-device cap - admins are few, and its "log out a device"
 * screen is built around user accounts.
 */
@Service
public class SystemAdminSessionServiceImpl implements SystemAdminSessionService {

    private static final long TOUCH_THROTTLE_MINUTES = 5;

    private final SystemAdminSessionRepository sessionRepository;

    // A row older than the JWT lifetime can no longer back a valid token.
    @Value("${jwt.expiration:15552000000}")
    private long sessionTtlMillis;

    public SystemAdminSessionServiceImpl(SystemAdminSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public SystemAdminSession createSession(SystemAdmin admin, String userAgent, String ipAddress) {
        sessionRepository.deleteByCreatedAtBefore(LocalDateTime.now().minus(sessionTtlMillis, ChronoUnit.MILLIS));

        // Logging in again from the same browser reuses its row rather than piling up new ones.
        SystemAdminSession session = sessionRepository
                .findByAdmin_IdAndUserAgentAndIpAddress(admin.getId(), userAgent, ipAddress)
                .orElseGet(() -> {
                    SystemAdminSession fresh = new SystemAdminSession();
                    fresh.setAdmin(admin);
                    fresh.setUserAgent(userAgent);
                    fresh.setIpAddress(ipAddress);
                    return fresh;
                });
        session.setJti(UUID.randomUUID().toString());
        session.setLastSeenAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Override
    public boolean isActive(String jti) {
        return jti != null && sessionRepository.findByJti(jti).isPresent();
    }

    @Override
    @Transactional
    public void touch(String jti) {
        if (jti == null) return;
        sessionRepository.findByJti(jti).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            if (ChronoUnit.MINUTES.between(session.getLastSeenAt(), now) >= TOUCH_THROTTLE_MINUTES) {
                session.setLastSeenAt(now);
                sessionRepository.save(session);
            }
        });
    }

    @Override
    @Transactional
    public void deleteByJti(String jti) {
        if (jti != null) {
            sessionRepository.deleteByJti(jti);
        }
    }

    @Override
    @Transactional
    public void deleteAllForAdmin(Long adminId) {
        sessionRepository.deleteByAdmin_Id(adminId);
    }
}
