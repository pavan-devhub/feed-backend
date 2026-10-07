package com.feedstartup.dto;

import java.util.List;
import java.util.Map;

/**
 * What the filters over one tab of an EPM list (upcoming or previous) can offer, now that the list
 * itself comes a page at a time: every place an EPM in the tab is held - for the cascading State /
 * District / Place dropdowns - and how many of its EPMs each category has, for the directory's
 * category sidebar. {@code total} counts the tab's EPMs before any other filter.
 */
public record EpmEventFacetsDto(long total, List<Place> places, Map<String, Long> categoryCounts) {

    public record Place(String state, String district, String city) {}
}
