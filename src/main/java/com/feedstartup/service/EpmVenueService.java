package com.feedstartup.service;

import com.feedstartup.dto.EpmVenueDto;
import com.feedstartup.dto.EpmVenueRequestDto;

import java.util.List;

/** The admin's reusable list of EPM venues (state / district / place / venue name). */
public interface EpmVenueService {

    List<EpmVenueDto> list();

    EpmVenueDto create(EpmVenueRequestDto dto);

    EpmVenueDto update(Long id, EpmVenueRequestDto dto);

    void delete(Long id);
}
