package com.feedstartup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Public submission payload for the "Become an EPM Volunteer" form - field-for-field match of
 * EpmVolunteer.jsx's formData. Email and reason are optional, mirroring the frontend's own
 * validation. */
public class EpmVolunteerRequestDto {

    @NotNull(message = "Please select an EPM to volunteer for")
    private Long epmEventId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Please select your background")
    private String experience;

    private String reason;

    public Long getEpmEventId() { return epmEventId; }
    public void setEpmEventId(Long epmEventId) { this.epmEventId = epmEventId; }

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

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
