package com.feedstartup.service;

import com.feedstartup.dto.EpmMyActivitiesDto;
import com.feedstartup.dto.EpmSignUpDetailsDto;

/** A logged-in user's own EPM registrations and volunteer sign-ups, with each EPM's latest status. */
public interface EpmActivityService {

    /**
     * Only sign-ups for EPMs still to come (today or later - cancelled ones included), soonest first.
     *
     * @param email the logged-in account's email (the JWT principal)
     */
    EpmMyActivitiesDto forUser(String email);

    /**
     * The account's name, mobile number, email, state, district and participant type, for the
     * register / volunteer forms to start from - so a logged-in user doesn't type them in again.
     *
     * @param email the logged-in account's email (the JWT principal)
     */
    EpmSignUpDetailsDto signUpDetails(String email);
}
