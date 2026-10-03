package com.feedstartup.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** How the EPM forms' old free-text choices are filed under today's participant types. */
class UserLinksMigrationRunnerTest {

    private static final Set<String> TYPES =
            Set.of("institutional", "individual", "business collaborator", "student", "executive", "guest");

    @Test
    void oldRegistrationChoicesMapToTheClosestType() {
        assertEquals("individual", registration("Farmer"));
        assertEquals("institutional", registration("FPO"));
        assertEquals("institutional", registration(" shg "));
        assertEquals("business collaborator", registration("Exporter"));
        assertEquals("student", registration("Student"));      // already a type
        assertEquals("guest", registration("Other"));
        assertEquals("guest", registration(null));
    }

    @Test
    void oldVolunteerBackgroundsMapToTheClosestType() {
        assertEquals("student", volunteer("Student"));
        assertEquals("institutional", volunteer("Government / Institutional"));
        assertEquals("business collaborator", volunteer("Business / MSME"));
        assertEquals("individual", volunteer("Agriculture / Farming"));
        assertEquals("guest", volunteer("Something else entirely"));
    }

    private static String registration(String old) {
        return UserLinksMigrationRunner.participantTypeFor(old, UserLinksMigrationRunner.LEGACY_REGISTRATION_TYPES, TYPES);
    }

    private static String volunteer(String old) {
        return UserLinksMigrationRunner.participantTypeFor(old, UserLinksMigrationRunner.LEGACY_VOLUNTEER_TYPES, TYPES);
    }
}
