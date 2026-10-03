package com.feedstartup.service;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.EpmStatsDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EpmEventService {

    /**
     * Public listing, each EPM with its counts and change history (so a rescheduled or moved EPM can
     * say so). Cancelled EPMs are left out unless {@code includeCancelled} - which only applies to
     * upcoming ones: the register / volunteer pages list those too, marked cancelled.
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
     * @param includeCancelled also list upcoming EPMs the admin has cancelled
     */
    List<EpmEventDto> list(String status, String state, String district, String city, String category, Integer month,
                           Integer year, boolean includeCancelled);

    /**
     * Admin listing: same filters as {@link #list}, plus a free-text {@code query} over
     * title/place/venue, and cancelled events are included. Every row carries its registration
     * and volunteer counts.
     */
    List<EpmEventDto> adminList(String status, String query, String state, String district, String city,
                                String category, Integer month, Integer year);

    EpmEventDto getById(Long id);

    EpmEventDto create(EpmEventRequestDto dto);

    /**
     * Saves the edit and logs each change to the date, time, place or description (see
     * EpmEventChanges). Only upcoming EPMs can be edited, and not moved to a date that has passed.
     */
    EpmEventDto update(Long id, EpmEventRequestDto dto);

    /** Calls an upcoming EPM off, logging the optional {@code reason} for the people registered or volunteering. */
    EpmEventDto cancel(Long id, String reason);

    /** Reinstates a cancelled upcoming EPM. */
    EpmEventDto restore(Long id);

    void delete(Long id);

    /** The given EPMs (missing ids are skipped), each with its net changes and full update log. */
    Map<Long, EpmEventDto> findWithHistory(Collection<Long> ids);

    EpmStatsDto getStats();

    /** The admin-managed categories, in display order. */
    List<EpmCategoryDto> listCategories();

    /**
     * Every distinct state / district / place / venue already entered - on any EPM (upcoming,
     * previous or cancelled) or in the venue list - sorted, with case-only duplicates merged.
     */
    List<EpmLocationDto> listLocations();
}
