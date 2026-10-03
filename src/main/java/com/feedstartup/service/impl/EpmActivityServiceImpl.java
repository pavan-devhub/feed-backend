package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmActivityDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmMyActivitiesDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmRegistration;
import com.feedstartup.model.EpmVolunteer;
import com.feedstartup.model.User;
import com.feedstartup.model.UserType;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.EpmActivityService;
import com.feedstartup.service.EpmEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Service
public class EpmActivityServiceImpl implements EpmActivityService {

    private final UserRepository userRepository;
    private final EpmRegistrationRepository registrationRepository;
    private final EpmVolunteerRepository volunteerRepository;
    private final EpmEventService epmEventService;

    @Autowired
    public EpmActivityServiceImpl(UserRepository userRepository,
                                  EpmRegistrationRepository registrationRepository,
                                  EpmVolunteerRepository volunteerRepository,
                                  EpmEventService epmEventService) {
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.volunteerRepository = volunteerRepository;
        this.epmEventService = epmEventService;
    }

    @Override
    public EpmMyActivitiesDto forUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<EpmRegistration> registrations = registrationRepository.findForUser(user.getId(), user.getEmail(), user.getPhone());
        List<EpmVolunteer> volunteers = volunteerRepository.findForUser(user.getId(), user.getEmail(), user.getPhone());

        Set<Long> eventIds = new HashSet<>();
        registrations.forEach(r -> eventIds.add(r.getEpmEventId()));
        volunteers.forEach(v -> eventIds.add(v.getEpmEventId()));
        Map<Long, EpmEventDto> events = epmEventService.findWithHistory(eventIds);

        // Only EPMs still to come (cancelled ones included, so people learn they're off) - past
        // ones drop off the dashboard.
        LocalDate today = LocalDate.now();
        Predicate<EpmActivityDto> upcoming = a -> !a.currentEventDate().isBefore(today);
        Comparator<EpmActivityDto> soonestFirst = Comparator.comparing(EpmActivityDto::currentEventDate);
        return new EpmMyActivitiesDto(
                registrations.stream()
                        .map(r -> new EpmActivityDto(r.getId(), r.getCreatedAt(), r.getFullName(), r.getMobileNumber(),
                                r.getEmail(), typeName(r.getParticipantType()), r.getEpmEventId(), r.getEventCity(),
                                r.getEventState(), r.getEventDate(), events.get(r.getEpmEventId())))
                        .filter(upcoming)
                        .sorted(soonestFirst)
                        .toList(),
                volunteers.stream()
                        .map(v -> new EpmActivityDto(v.getId(), v.getCreatedAt(), v.getFullName(), v.getMobileNumber(),
                                v.getEmail(), typeName(v.getParticipantType()), v.getEpmEventId(), v.getEventCity(),
                                v.getEventState(), v.getEventDate(), events.get(v.getEpmEventId())))
                        .filter(upcoming)
                        .sorted(soonestFirst)
                        .toList());
    }

    private static String typeName(UserType type) {
        return type == null ? null : type.getName();
    }
}
