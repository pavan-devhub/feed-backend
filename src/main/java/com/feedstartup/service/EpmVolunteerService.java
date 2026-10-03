package com.feedstartup.service;

import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.dto.EpmVolunteerRequestDto;
import com.feedstartup.dto.PageDto;

import java.util.List;

public interface EpmVolunteerService {

    /** @param userId the logged-in account sending the form, or null when signed out */
    EpmVolunteerDto volunteer(EpmVolunteerRequestDto dto, Long userId);

    /** One page of the admin list, newest first. */
    PageDto<EpmVolunteerDto> page(EpmSubmissionFilter filter, int page, int size);

    /** Every submission matching {@code filter}, newest first - for the admin's spreadsheet export. */
    List<EpmVolunteerDto> list(EpmSubmissionFilter filter);
}
