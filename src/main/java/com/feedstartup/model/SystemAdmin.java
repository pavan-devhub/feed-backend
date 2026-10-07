package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An account that runs the admin panel (Feed World publications and every EPM screen). Kept apart
 * from {@link User}: an admin has just a username, mobile number, email and password, and logs in
 * on the same login page with any one of the first three. The first admin is seeded by
 * SystemAdminBootstrapRunner; every other one is created by an existing admin from the panel.
 *
 * <p>Username, mobile number and email are each unique here - and, since any of them can be typed
 * on the login page, SystemAdminServiceImpl also keeps the mobile number and email clear of the
 * users table's (and registration keeps users clear of admins').
 */
@Entity
@Table(name = "system_admins")
public class SystemAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 10)
    private String mobileNumber;

    @Column(nullable = false, unique = true)
    private String email;

    // BCrypt hash, like User#password.
    @Column(nullable = false)
    private String password;

    // The admin who created this one - null for the seeded default admin.
    @ManyToOne
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_system_admins_created_by"))
    private SystemAdmin createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public SystemAdmin() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public SystemAdmin getCreatedBy() { return createdBy; }
    public void setCreatedBy(SystemAdmin createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
