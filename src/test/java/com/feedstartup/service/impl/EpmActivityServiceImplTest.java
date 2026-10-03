package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmActivityDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmMyActivitiesDto;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmRegistration;
import com.feedstartup.model.UserType;
import com.feedstartup.model.User;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.EpmEventService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A user's "Status of Activities" lists their own sign-ups for upcoming EPMs, soonest first. */
class EpmActivityServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EpmRegistrationRepository registrationRepository = mock(EpmRegistrationRepository.class);
    private final EpmVolunteerRepository volunteerRepository = mock(EpmVolunteerRepository.class);
    private final EpmEventService eventService = mock(EpmEventService.class);
    private final EpmActivityServiceImpl service =
            new EpmActivityServiceImpl(userRepository, registrationRepository, volunteerRepository, eventService);

    @Test
    void listsOnlyTheUsersUpcomingRegistrationsSoonestFirstAndKeepsRemovedEpms() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(5L);
        user.setEmail("ravi@example.com");
        user.setPhone("9876543210");
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(user));
        when(registrationRepository.findForUser(5L, "ravi@example.com", "9876543210")).thenReturn(List.of(
                registration(1L, 11L, today.minusDays(30)),
                registration(2L, 12L, today.plusDays(40)),
                registration(3L, 13L, today.plusDays(5)),   // removed by the admin since
                registration(4L, 14L, today.minusDays(3))));
        when(volunteerRepository.findForUser(5L, "ravi@example.com", "9876543210")).thenReturn(List.of());
        when(eventService.findWithHistory(any())).thenReturn(Map.of(
                11L, event(11L, today.minusDays(30)),
                // Rescheduled from 40 days out to tomorrow - its current date decides the order.
                12L, event(12L, today.plusDays(1)),
                14L, event(14L, today.minusDays(3))));

        EpmMyActivitiesDto mine = service.forUser("ravi@example.com");

        // 1 and 4 are past, so they drop off.
        assertEquals(List.of(2L, 3L), mine.registrations().stream().map(EpmActivityDto::id).toList());
        assertNull(mine.registrations().get(1).event());
        assertEquals(today.plusDays(5), mine.registrations().get(1).currentEventDate());
        assertTrue(mine.volunteers().isEmpty());
    }

    private static EpmRegistration registration(Long id, Long eventId, LocalDate eventDate) {
        EpmRegistration r = new EpmRegistration();
        r.setId(id);
        r.setEpmEventId(eventId);
        r.setEventDate(eventDate);
        r.setEventCity("Vijayawada");
        r.setEventState("Andhra Pradesh");
        r.setFullName("Ravi");
        r.setMobileNumber("9876543210");
        r.setParticipantType(new UserType("Individual", 0));
        return r;
    }

    private static EpmEventDto event(Long id, LocalDate date) {
        EpmEvent e = new EpmEvent();
        e.setId(id);
        e.setTitle("EPM " + id);
        e.setEventDate(date);
        return EpmEventDto.from(e);
    }
}
