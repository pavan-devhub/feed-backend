package com.feedstartup.dto;

/**
 * A logged-in user's own details, in the shape of the "Register for EPM" / "Become an EPM
 * Volunteer" forms, so those forms open already filled in from the account. {@code participantType}
 * is null when the account's user type isn't one of the EPM participant types (the form then asks).
 */
public record EpmSignUpDetailsDto(String fullName, String mobileNumber, String email, String state,
                                  String district, String participantType) {}
