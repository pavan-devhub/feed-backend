package com.feedstartup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One of a user's own EPM registrations or volunteer sign-ups, for their dashboard's "Status of
 * Activities". {@code event} is the EPM as it stands now, with its change history - or null if the
 * admin has since removed it, leaving only the copied {@code eventCity}/{@code eventState}/{@code eventDate}.
 * {@code participantType} is the participant type chosen on the form (one of the user_types).
 */
public record EpmActivityDto(
        Long id,
        LocalDateTime registeredAt,
        String fullName,
        String mobileNumber,
        String email,
        String participantType,
        Long epmEventId,
        String eventCity,
        String eventState,
        LocalDate eventDate,
        EpmEventDto event) {

    /** The EPM's date as it stands now, falling back to the copy kept with the submission. */
    public LocalDate currentEventDate() {
        return event != null && event.getEventDate() != null ? event.getEventDate() : eventDate;
    }
}
