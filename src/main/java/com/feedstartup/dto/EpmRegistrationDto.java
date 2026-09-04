package com.feedstartup.dto;

import com.feedstartup.model.EpmRegistration;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Admin read shape listing a submitted EPM registration. */
public class EpmRegistrationDto {

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
    private String participantType;
    private boolean consent;
    private LocalDateTime createdAt;

    public static EpmRegistrationDto from(EpmRegistration r) {
        EpmRegistrationDto dto = new EpmRegistrationDto();
        dto.id = r.getId();
        dto.epmEventId = r.getEpmEventId();
        dto.eventCity = r.getEventCity();
        dto.eventState = r.getEventState();
        dto.eventDate = r.getEventDate();
        dto.fullName = r.getFullName();
        dto.mobileNumber = r.getMobileNumber();
        dto.email = r.getEmail();
        dto.state = r.getState();
        dto.district = r.getDistrict();
        dto.participantType = r.getParticipantType();
        dto.consent = r.isConsent();
        dto.createdAt = r.getCreatedAt();
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
    public String getParticipantType() { return participantType; }
    public boolean isConsent() { return consent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
