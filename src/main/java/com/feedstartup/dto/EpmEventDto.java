package com.feedstartup.dto;

import com.feedstartup.model.EpmEvent;

import java.time.LocalDate;
import java.util.List;

/** Public read shape for an EpmEvent - powers the EPM page, details/filter screen, the location
 * picker on the register/volunteer forms, the admin panel's event table and the user dashboard's
 * activity status. The two counts and the change history are only filled in by the views that ask
 * for them (see EpmEventServiceImpl#withCounts and #findWithHistory). */
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
    // Which tracked fields differ from when the EPM was first scheduled (e.g. ["date", "venue"]),
    // and every logged change, newest first - see EpmEventChanges.
    private List<String> changes;
    private List<EpmEventUpdateDto> updates;

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

    public EpmEventDto withHistory(List<String> changes, List<EpmEventUpdateDto> updates) {
        this.changes = changes;
        this.updates = updates;
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
    public List<String> getChanges() { return changes; }
    public List<EpmEventUpdateDto> getUpdates() { return updates; }
}
