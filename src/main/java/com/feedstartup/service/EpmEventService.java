package com.feedstartup.service;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventFacetsDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.dto.PageDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EpmEventService {

    /**
     * Every EPM matching {@code filter} (see EpmEventFilter#forPublic and #forAdmin), each with its
     * registration and volunteer counts and its change history - so a rescheduled or moved EPM can
     * say so. Upcoming EPMs come soonest first, previous ones latest first.
     */
    List<EpmEventDto> list(EpmEventFilter filter);

    /** The same list, a page at a time - {@code page} is 0-based and {@code size} at most Paging.MAX_PAGE_SIZE. */
    PageDto<EpmEventDto> page(EpmEventFilter filter, int page, int size);

    /**
     * The choices for the filters over {@code filter}'s tab - its status and cancelled rule, every
     * other filter ignored: the places its EPMs are held and how many each category has.
     */
    EpmEventFacetsDto facets(EpmEventFilter filter);

    /** One EPM, with its counts and change history. */
    EpmEventDto getById(Long id);

    // Each of these admin actions is also written to the EPM Activity log (EpmAdminActivityService)
    // under {@code adminId}, the logged-in admin.

    EpmEventDto create(EpmEventRequestDto dto, Long adminId);

    /**
     * Saves the edit and logs each change to the date, time, place or description (see
     * EpmEventChanges). Only upcoming EPMs can be edited, and not moved to a date that has passed.
     */
    EpmEventDto update(Long id, EpmEventRequestDto dto, Long adminId);

    /** Calls an upcoming EPM off, logging the optional {@code reason} for the people registered or volunteering. */
    EpmEventDto cancel(Long id, String reason, Long adminId);

    /** Reinstates a cancelled upcoming EPM. */
    EpmEventDto restore(Long id, Long adminId);

    void delete(Long id, Long adminId);

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
