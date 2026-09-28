package com.feedstartup.dto;

import com.feedstartup.model.EpmCategory;

/** Public read shape for a category option - backs the EPM details page's "Filter by Category"
 * sidebar. {@code id} is the category's name, the same string stored on EpmEventDto#category. */
public record EpmCategoryDto(String id, String label, String color) {

    public static EpmCategoryDto from(EpmCategory category) {
        return new EpmCategoryDto(category.getName(), category.getLabel(), category.getColor());
    }
}
