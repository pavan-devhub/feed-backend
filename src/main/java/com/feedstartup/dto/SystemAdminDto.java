package com.feedstartup.dto;

import com.feedstartup.model.SystemAdmin;

import java.time.LocalDateTime;

/**
 * One row of the admin panel's "System Admins" list. {@code createdBy} is the creating admin's
 * username - null for the default admin, which was seeded rather than created by anyone.
 */
public record SystemAdminDto(Long id, String username, String mobileNumber, String email,
                             String createdBy, LocalDateTime createdAt, boolean defaultAdmin) {

    public static SystemAdminDto from(SystemAdmin admin) {
        SystemAdmin creator = admin.getCreatedBy();
        return new SystemAdminDto(admin.getId(), admin.getUsername(), admin.getMobileNumber(), admin.getEmail(),
                creator == null ? null : creator.getUsername(), admin.getCreatedAt(), creator == null);
    }
}
