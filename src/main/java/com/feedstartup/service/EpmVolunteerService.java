package com.feedstartup.service;

import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.dto.EpmVolunteerRequestDto;

import java.util.List;

public interface EpmVolunteerService {

    EpmVolunteerDto volunteer(EpmVolunteerRequestDto dto);

    List<EpmVolunteerDto> list(Long epmEventId);
}
