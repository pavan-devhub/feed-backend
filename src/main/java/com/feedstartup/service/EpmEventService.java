package com.feedstartup.service;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmStatsDto;

import java.util.List;

public interface EpmEventService {

    /**
     * Public listing - cancelled events are always left out.
     *
     * @param status one of "upcoming" (default), "previous" or "all"
     * @param state optional exact-match filter (case-insensitive)
     * @param district optional exact-match filter (case-insensitive)
     * @param city optional exact-match filter (case-insensitive)
     * @param category optional exact-match filter (case-insensitive) against a category name
     * @param month optional 1-12 filter on the event's calendar month
     * @param year optional calendar-year filter (e.g. the homepage calendar widget asks for a
     *             specific year+month together, since a bare month would otherwise match that
     *             month across every year in the data)
     */
    List<EpmEventDto> list(String status, String state, String district, String city, String category, Integer month, Integer year);

    /**
     * Admin listing: same filters as {@link #list}, plus a free-text {@code query} over
     * title/place/venue, and cancelled events are included. Every row carries its registration
     * and volunteer counts.
     */
    List<EpmEventDto> adminList(String status, String query, String state, String district, String city,
                                String category, Integer month, Integer year);

    EpmEventDto getById(Long id);

    EpmEventDto create(EpmEventRequestDto dto);

    EpmEventDto update(Long id, EpmEventRequestDto dto);

    void delete(Long id);

    EpmStatsDto getStats();

    /** The admin-managed categories, in display order. */
    List<EpmCategoryDto> listCategories();
}
