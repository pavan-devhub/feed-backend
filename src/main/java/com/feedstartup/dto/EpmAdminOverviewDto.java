package com.feedstartup.dto;

/** Headline numbers for the admin panel's EPM overview. */
public record EpmAdminOverviewDto(long upcomingEvents, long previousEvents, long cancelledEvents,
                                  long registrations, long registrationsToday,
                                  long volunteers, long volunteersToday,
                                  long galleryImages, long reviews, long categories, long venues) {}
