package com.feedstartup.dto;

/** One state / district / place / venue combination already in use - from an EPM (upcoming,
 * previous or cancelled) or the venue list. Feeds the suggestions on the admin's EPM form. */
public record EpmLocationDto(String state, String district, String city, String venue) {}
