package com.feedstartup.service;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmStatsDto;

import java.util.List;

public interface EpmEventService {

    /**
     * @param status one of "upcoming" (default), "previous" or "all"
     * @param state optional exact-match filter (case-insensitive)
     * @param district optional exact-match filter (case-insensitive)
     * @param city optional exact-match filter (case-insensitive)
     * @param category optional exact-match filter (case-insensitive) against one of the fixed
     *                 EpmCategory ids
     * @param month optional 1-12 filter on the event's calendar month
     * @param year optional calendar-year filter (e.g. the homepage calendar widget asks for a
     *             specific year+month together, since a bare month would otherwise match that
     *             month across every year in the data)
     */
    List<EpmEventDto> list(String status, String state, String district, String city, String category, Integer month, Integer year);

    EpmEventDto getById(Long id);

    EpmEventDto create(EpmEventRequestDto dto);

    EpmEventDto update(Long id, EpmEventRequestDto dto);

    void delete(Long id);

    EpmStatsDto getStats();

    /** The fixed list of allowed event categories, in display order. */
    List<EpmCategoryDto> listCategories();
}
