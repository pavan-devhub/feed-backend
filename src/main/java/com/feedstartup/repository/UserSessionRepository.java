package com.feedstartup.repository;

import com.feedstartup.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    long countByUserId(Long userId);
    List<UserSession> findByUserIdOrderByCreatedAtAsc(Long userId);
    Optional<UserSession> findByJti(String jti);
    Optional<UserSession> findByIdAndUserId(Long id, Long userId);
    Optional<UserSession> findByUserIdAndUserAgentAndIpAddress(Long userId, String userAgent, String ipAddress);
    void deleteByJti(String jti);
    void deleteByUserIdAndCreatedAtBefore(Long userId, LocalDateTime cutoff);
}
