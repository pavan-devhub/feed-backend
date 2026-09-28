package com.feedstartup.service;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmRegistrationRequestDto;

import java.time.LocalDate;
import java.util.List;

public interface EpmRegistrationService {

    EpmRegistrationDto register(EpmRegistrationRequestDto dto);

    /**
     * Admin listing, newest first. Every filter is optional and they combine:
     * @param epmEventId only submissions for this EPM
     * @param eventDate only submissions for EPMs held on this date
     * @param submittedOn only submissions made on this date
     * @param query free-text match on name, mobile number, email, state, district or the EPM's city
     */
    List<EpmRegistrationDto> list(Long epmEventId, LocalDate eventDate, LocalDate submittedOn, String query);
}
