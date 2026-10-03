package com.feedstartup.service;

import com.feedstartup.dto.EpmMyActivitiesDto;

/** A logged-in user's own EPM registrations and volunteer sign-ups, with each EPM's latest status. */
public interface EpmActivityService {

    /**
     * Only sign-ups for EPMs still to come (today or later - cancelled ones included), soonest first.
     *
     * @param email the logged-in account's email (the JWT principal)
     */
    EpmMyActivitiesDto forUser(String email);
}
