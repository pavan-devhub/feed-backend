package com.feedstartup.service;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmRegistrationRequestDto;
import com.feedstartup.dto.PageDto;

import java.util.List;

public interface EpmRegistrationService {

    /** @param userId the logged-in account sending the form, or null when signed out */
    EpmRegistrationDto register(EpmRegistrationRequestDto dto, Long userId);

    /** One page of the admin list, newest first. */
    PageDto<EpmRegistrationDto> page(EpmSubmissionFilter filter, int page, int size);

    /** Every submission matching {@code filter}, newest first - for the admin's spreadsheet export. */
    List<EpmRegistrationDto> list(EpmSubmissionFilter filter);
}
