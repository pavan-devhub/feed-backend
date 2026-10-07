package com.feedstartup.service;

import com.feedstartup.dto.SystemAdminDto;
import com.feedstartup.dto.SystemAdminRequestDto;
import com.feedstartup.model.SystemAdmin;

import java.util.List;
import java.util.Optional;

/** The accounts that run the admin panel - backed by the {@code system_admins} table. */
public interface SystemAdminService {

    /** Every admin, the default one first, then in the order they were created. */
    List<SystemAdminDto> list();

    /**
     * Adds an admin. Throws AlreadyTakenException naming whichever of the username, mobile number
     * and email is already in use - by another admin, or (mobile number and email) by a user,
     * since the login page takes any of them.
     *
     * @param createdByAdminId the logged-in admin creating it
     */
    SystemAdminDto create(SystemAdminRequestDto dto, Long createdByAdminId);

    /**
     * Removes an admin and logs them out everywhere. Only the default admin - the one the system
     * set up, not added by anyone - may do this (ForbiddenException otherwise), and the default
     * admin itself can never be removed (ConflictException).
     *
     * @param requestedByAdminId the logged-in admin asking for it
     */
    void delete(Long id, Long requestedByAdminId);

    /**
     * The admin whose username, email or mobile number is {@code login} - empty if none is (the
     * login is then a user's). Throws IllegalArgumentException("Invalid password") if there is
     * one but {@code password} isn't theirs.
     */
    Optional<SystemAdmin> authenticate(String login, String password);

    SystemAdmin get(Long id);
}
