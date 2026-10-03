package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A submission of the "Become an EPM Volunteer" form. Mirrors {@link EpmRegistration} - see its
 * javadoc for why the event's city/state/date are snapshotted rather than only referenced by id.
 */
@Entity
@Table(name = "epm_volunteers")
public class EpmVolunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epm_event_id", nullable = false)
    private Long epmEventId;

    // The account this sign-up belongs to - see EpmRegistration#userId (same foreign key to users).
    @Column(name = "user_id")
    private Long userId;

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

    // Same as EpmRegistration#participantType: a foreign key to user_types. (The form used to ask
    // for a free-text "experience" instead; UserLinksMigrationRunner kept those as legacy_experience.)
    @ManyToOne
    @JoinColumn(name = "participant_type_id", foreignKey = @ForeignKey(name = "fk_epm_volunteers_participant_type"))
    private UserType participantType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public EpmVolunteer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public UserType getParticipantType() { return participantType; }
    public void setParticipantType(UserType participantType) { this.participantType = participantType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
