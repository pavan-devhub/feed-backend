package com.feedstartup.service;

import com.feedstartup.dto.UserTypeDto;

import java.util.List;

/** The user types people can register as - backed by the {@code user_types} table. */
public interface UserTypeService {

    /** The active types, in the order the registration form lists them. */
    List<UserTypeDto> listActive();

    /**
     * The canonical name of the active type {@code requested} names, matched ignoring case (so
     * "student" is stored as "Student"). Throws IllegalArgumentException, listing the valid
     * choices, if it names no active type.
     */
    String resolveActive(String requested);
}
