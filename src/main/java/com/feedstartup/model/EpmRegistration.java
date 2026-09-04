package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A participant's submission of the "Register for EPM" form. The event's city/state/date are
 * copied in at submission time (not just referenced by {@link #epmEventId}) so this record still
 * reads correctly even if that EpmEvent is later edited or removed.
 */
@Entity
@Table(name = "epm_registrations")
public class EpmRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epm_event_id", nullable = false)
    private Long epmEventId;

    @Column(name = "event_city", nullable = false)
    private String eventCity;

    @Column(name = "event_state", nullable = false)
    private String eventState;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    private String email;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String district;

    @Column(name = "participant_type", nullable = false)
    private String participantType;

    @Column(nullable = false)
    private boolean consent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public EpmRegistration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEpmEventId() { return epmEventId; }
    public void setEpmEventId(Long epmEventId) { this.epmEventId = epmEventId; }

    public String getEventCity() { return eventCity; }
    public void setEventCity(String eventCity) { this.eventCity = eventCity; }

    public String getEventState() { return eventState; }
    public void setEventState(String eventState) { this.eventState = eventState; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getParticipantType() { return participantType; }
    public void setParticipantType(String participantType) { this.participantType = participantType; }

    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
