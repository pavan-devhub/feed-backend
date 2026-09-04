package com.feedstartup.dto;

import com.feedstartup.model.EpmVolunteer;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Admin read shape listing a submitted EPM volunteer application. */
public class EpmVolunteerDto {

    private Long id;
    private Long epmEventId;
    private String eventCity;
    private String eventState;
    private LocalDate eventDate;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String state;
    private String district;
    private String experience;
    private String reason;
    private LocalDateTime createdAt;

    public static EpmVolunteerDto from(EpmVolunteer v) {
        EpmVolunteerDto dto = new EpmVolunteerDto();
        dto.id = v.getId();
        dto.epmEventId = v.getEpmEventId();
        dto.eventCity = v.getEventCity();
        dto.eventState = v.getEventState();
        dto.eventDate = v.getEventDate();
        dto.fullName = v.getFullName();
        dto.mobileNumber = v.getMobileNumber();
        dto.email = v.getEmail();
        dto.state = v.getState();
        dto.district = v.getDistrict();
        dto.experience = v.getExperience();
        dto.reason = v.getReason();
        dto.createdAt = v.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getEpmEventId() { return epmEventId; }
    public String getEventCity() { return eventCity; }
    public String getEventState() { return eventState; }
    public LocalDate getEventDate() { return eventDate; }
    public String getFullName() { return fullName; }
    public String getMobileNumber() { return mobileNumber; }
    public String getEmail() { return email; }
    public String getState() { return state; }
    public String getDistrict() { return district; }
    public String getExperience() { return experience; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
