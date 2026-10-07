package com.feedstartup.service;

import com.feedstartup.dto.UserTypeDto;
import com.feedstartup.model.UserType;

import java.util.List;
import java.util.Optional;

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

    /** The EPM forms' "Participant Type" choices (UserType#epmOrder), in dropdown order. */
    List<UserTypeDto> listEpmParticipantTypes();

    /**
     * The participant type {@code requested} names, matched ignoring case. Throws
     * IllegalArgumentException, listing the valid choices, if it isn't one the EPM forms offer.
     */
    UserType resolveEpmParticipantType(String requested);

    /**
     * The participant type {@code name} names, matched ignoring case - or empty if it isn't one the
     * EPM forms offer (e.g. an account's "Government" user type).
     */
    Optional<UserType> findEpmParticipantType(String name);
}
