package com.feedstartup.dto;

import com.feedstartup.model.EpmEvent;

import java.time.LocalDate;

/** Public read shape for an EpmEvent - powers the EPM page, details/filter screen, the location
 * picker on the register/volunteer forms, and the admin panel's event table. The two counts are
 * only filled in by list views that ask for them (see EpmEventServiceImpl#withCounts). */
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
    private String description;
    private boolean upcoming;
    private boolean cancelled;
    private Long registrationCount;
    private Long volunteerCount;

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
        dto.description = e.getDescription();
        dto.upcoming = e.getEventDate() != null && !e.getEventDate().isBefore(LocalDate.now());
        dto.cancelled = e.isCancelled();
        return dto;
    }

    public EpmEventDto withCounts(long registrations, long volunteers) {
        this.registrationCount = registrations;
        this.volunteerCount = volunteers;
        return this;
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
    public String getDescription() { return description; }
    public boolean isUpcoming() { return upcoming; }
    public boolean isCancelled() { return cancelled; }
    public Long getRegistrationCount() { return registrationCount; }
    public Long getVolunteerCount() { return volunteerCount; }
}
