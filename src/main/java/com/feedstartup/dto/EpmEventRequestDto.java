package com.feedstartup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin create/update payload for an EpmEvent. {@code eventDate} is a plain "yyyy-MM-dd" string
 * (parsed in the service), matching how dates are already handled in UserRegistrationDto#dob. */
public class EpmEventRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    // Must match the name of one of the admin-managed categories (see GET /api/epm/events/categories)
    // - enforced in EpmEventServiceImpl#resolveCategory, since the allowed list lives in the database.
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

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }
}
