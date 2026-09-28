package com.feedstartup.dto;

import com.feedstartup.model.EpmCategory;

/** Admin read shape for a category, with how many events use it. */
public record EpmCategoryAdminDto(Long id, String name, String label, String color, int displayOrder,
                                  long eventCount, long upcomingEventCount) {

    public static EpmCategoryAdminDto from(EpmCategory c, long eventCount, long upcomingEventCount) {
        return new EpmCategoryAdminDto(c.getId(), c.getName(), c.getLabel(), c.getColor(), c.getDisplayOrder(),
                eventCount, upcomingEventCount);
    }
}
