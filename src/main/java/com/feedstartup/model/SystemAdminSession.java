package com.feedstartup.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One device a {@link SystemAdmin} is logged in on - the admin counterpart of {@link UserSession}
 * (whose user_id is a foreign key to users, so it can't hold admins). An admin's JWT is only
 * honoured while its jti has a row here, and that is also what grants it ROLE_ADMIN (see
 * JwtAuthenticationFilter) - logging out deletes the row.
 */
@Entity
@Table(name = "system_admin_sessions")
public class SystemAdminSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id", nullable = false, foreignKey = @ForeignKey(name = "fk_system_admin_sessions_admin"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SystemAdmin admin;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeenAt = createdAt;
    }

    public SystemAdminSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SystemAdmin getAdmin() { return admin; }
    public void setAdmin(SystemAdmin admin) { this.admin = admin; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
