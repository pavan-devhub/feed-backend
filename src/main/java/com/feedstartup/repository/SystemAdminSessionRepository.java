package com.feedstartup.repository;

import com.feedstartup.model.SystemAdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SystemAdminSessionRepository extends JpaRepository<SystemAdminSession, Long> {
    Optional<SystemAdminSession> findByJti(String jti);
    Optional<SystemAdminSession> findByAdmin_IdAndUserAgentAndIpAddress(Long adminId, String userAgent, String ipAddress);
    void deleteByJti(String jti);
    void deleteByAdmin_Id(Long adminId);
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
