package com.feedstartup.service;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmRegistrationRequestDto;

import java.util.List;

public interface EpmRegistrationService {

    EpmRegistrationDto register(EpmRegistrationRequestDto dto);

    List<EpmRegistrationDto> list(Long epmEventId);
}
