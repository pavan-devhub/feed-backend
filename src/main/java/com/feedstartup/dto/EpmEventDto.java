package com.feedstartup.dto;

import com.feedstartup.model.EpmEvent;

import java.time.LocalDate;

/** Public read shape for an EpmEvent - powers the EPM page, details/filter screen, and the
 * location picker on the register/volunteer forms. */
public class EpmEventDto {

    private Long id;
    private String title;
    private String category;
    private String state;
    private String district;
    private String city;
    private String venue;
    private LocalDate eventDate;
    private String timeRange;
    private boolean upcoming;
    private boolean cancelled;

    public static EpmEventDto from(EpmEvent e) {
        EpmEventDto dto = new EpmEventDto();
        dto.id = e.getId();
        dto.title = e.getTitle();
        dto.category = e.getCategory();
        dto.state = e.getState();
        dto.district = e.getDistrict();
        dto.city = e.getCity();
        dto.venue = e.getVenue();
        dto.eventDate = e.getEventDate();
        dto.timeRange = e.getTimeRange();
        dto.upcoming = e.getEventDate() != null && !e.getEventDate().isBefore(LocalDate.now());
        dto.cancelled = e.isCancelled();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getState() { return state; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public String getVenue() { return venue; }
    public LocalDate getEventDate() { return eventDate; }
    public String getTimeRange() { return timeRange; }
    public boolean isUpcoming() { return upcoming; }
    public boolean isCancelled() { return cancelled; }
}
