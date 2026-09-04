package com.feedstartup.dto;

import com.feedstartup.model.EpmCategory;

/** Public read shape for a category option - backs the EPM details page's "Filter by Category"
 * sidebar so the list of categories is driven by the backend rather than hardcoded in the UI. */
public class EpmCategoryDto {

    private String id;
    private String label;

    public static EpmCategoryDto from(EpmCategory category) {
        EpmCategoryDto dto = new EpmCategoryDto();
        dto.id = category.getId();
        dto.label = category.getLabel();
        return dto;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
}
