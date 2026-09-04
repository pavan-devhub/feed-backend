package com.feedstartup.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin create/update payload for an EpmEvent. {@code eventDate} is a plain "yyyy-MM-dd" string
 * (parsed in the service), matching how dates are already handled in UserRegistrationDto#dob. */
public class EpmEventRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    // Must match one of the EpmCategory ids (see GET /api/epm/events/categories) - enforced in
    // EpmEventServiceImpl#resolveCategory rather than here, since the message needs the enum's
    // current list of allowed ids.
    private String category;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Venue is required")
    private String venue;

    @NotBlank(message = "Event date is required")
    private String eventDate;

    private String timeRange;

    private Boolean cancelled;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }
}
