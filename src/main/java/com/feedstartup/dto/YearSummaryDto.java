package com.feedstartup.dto;

/** Powers the collapsible year groups in the publication archive sidebar. */
public class YearSummaryDto {

    private final Integer year;
    private final Long count;

    public YearSummaryDto(Integer year, Long count) {
        this.year = year;
        this.count = count;
    }

    public Integer getYear() { return year; }
    public Long getCount() { return count; }
}
