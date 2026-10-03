package com.feedstartup.dto;

import java.util.List;

/** Everything on a user's "Status of Activities": the EPMs they registered for and volunteered at. */
public record EpmMyActivitiesDto(List<EpmActivityDto> registrations, List<EpmActivityDto> volunteers) {}
