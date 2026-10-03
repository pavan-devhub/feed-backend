package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmEventUpdate.Field;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmCategoryService;
import com.feedstartup.service.EpmVenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Admin edits and cancellations of an upcoming EPM are logged for the people signed up to it; a
 * previous EPM can't be edited, cancelled or reinstated at all.
 */
class EpmEventServiceImplHistoryTest {

    private static final LocalDate EVENT_DATE = LocalDate.now().plusDays(13);
    private static final LocalDate LATER = EVENT_DATE.plusDays(5);

    private final EpmEventRepository eventRepository = mock(EpmEventRepository.class);
    private final EpmRegistrationRepository registrationRepository = mock(EpmRegistrationRepository.class);
    private final EpmVolunteerRepository volunteerRepository = mock(EpmVolunteerRepository.class);
    private final EpmCategoryService categoryService = mock(EpmCategoryService.class);
    private final EpmEventUpdateRepository updateRepository = mock(EpmEventUpdateRepository.class);
    private final EpmEventServiceImpl service = new EpmEventServiceImpl(eventRepository, registrationRepository,
            volunteerRepository, categoryService, mock(EpmVenueService.class), updateRepository);

    private final EpmEvent event = new EpmEvent();
    private final List<EpmEventUpdate> logged = new ArrayList<>();

    @BeforeEach
    void setUp() {
        event.setId(7L);
        event.setTitle("Export Promotion Meeting");
        event.setCategory("EPM Meeting");
        event.setState("Delhi");
        event.setDistrict("New Delhi");
        event.setCity("New Delhi");
        event.setVenue("Pragati Maidan");
        event.setEventDate(EVENT_DATE);
        event.setTimeRange("09:00 AM - 04:00 PM");
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));
        when(eventRepository.findAllById(any())).thenReturn(List.of(event));
        when(eventRepository.save(any(EpmEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryService.resolveName(any())).thenReturn("EPM Meeting");
        when(updateRepository.saveAll(any())).thenAnswer(inv -> {
            ((Iterable<EpmEventUpdate>) inv.getArgument(0)).forEach(logged::add);
            return inv.getArgument(0);
        });
        when(updateRepository.save(any(EpmEventUpdate.class))).thenAnswer(inv -> {
            logged.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(updateRepository.findByEpmEventIdInOrderByCreatedAtAscIdAsc(anyCollection())).thenAnswer(inv -> logged);
    }

    @Test
    void editingTheVenueAndTimeLogsBothAndTheStatusSaysWhatChanged() {
        service.update(7L, request("Bharat Mandapam", EVENT_DATE.toString(), "10:00 AM - 05:00 PM"));

        assertEquals(List.of(Field.TIME, Field.VENUE), logged.stream().map(EpmEventUpdate::getField).toList());
        assertEquals("Pragati Maidan", logged.get(1).getOldValue());
        assertEquals("Bharat Mandapam", logged.get(1).getNewValue());
        // Place and date didn't move, so the registrations' copy is left alone.
        verify(registrationRepository, never()).syncEventSnapshot(anyLong(), anyString(), anyString(), any());

        EpmEventDto dto = service.findWithHistory(Set.of(7L)).get(7L);
        assertEquals(List.of("time", "venue"), dto.getChanges());
        assertEquals("venue", dto.getUpdates().get(0).field()); // newest first
    }

    @Test
    void reschedulingMovesTheRegistrationsAndVolunteersCopyOfTheDateToo() {
        service.update(7L, request("Pragati Maidan", LATER.toString(), "09:00 AM - 04:00 PM"));

        verify(registrationRepository).syncEventSnapshot(7L, "New Delhi", "Delhi", LATER);
        verify(volunteerRepository).syncEventSnapshot(7L, "New Delhi", "Delhi", LATER);
    }

    @Test
    void cancellingLogsTheReasonAndCannotBeDoneTwice() {
        EpmEventDto cancelled = service.cancel(7L, "  Heavy rain forecast ");

        assertTrue(cancelled.isCancelled());
        ArgumentCaptor<EpmEventUpdate> saved = ArgumentCaptor.forClass(EpmEventUpdate.class);
        verify(updateRepository).save(saved.capture());
        assertEquals(Field.CANCELLED, saved.getValue().getField());
        assertEquals("Heavy rain forecast", saved.getValue().getNewValue());

        assertThrows(ConflictException.class, () -> service.cancel(7L, null));
    }

    @Test
    void restoringACancelledEventLogsIt() {
        service.cancel(7L, " ");
        assertNull(logged.get(0).getNewValue());

        assertFalse(service.restore(7L).isCancelled());
        assertEquals(Field.RESTORED, logged.get(1).getField());
        assertThrows(ConflictException.class, () -> service.restore(7L));
    }

    @Test
    void anEditThatChangesNothingLogsNothing() {
        service.update(7L, request("PRAGATI MAIDAN", EVENT_DATE.toString(), "09:00 AM - 04:00 PM"));
        assertTrue(logged.isEmpty());
    }

    @Test
    void aPreviousEpmCannotBeEditedCancelledOrReinstated() {
        event.setEventDate(LocalDate.now().minusDays(1));

        assertThrows(ConflictException.class, () -> service.update(7L, request("Bharat Mandapam", LATER.toString(), null)));
        assertThrows(ConflictException.class, () -> service.cancel(7L, null));
        event.setCancelled(true);
        assertThrows(ConflictException.class, () -> service.restore(7L));
        assertTrue(logged.isEmpty());
        verify(eventRepository, never()).save(any(EpmEvent.class));
    }

    @Test
    void anEpmHeldTodayIsStillUpcomingAndEditable() {
        event.setEventDate(LocalDate.now());
        service.update(7L, request("Bharat Mandapam", LocalDate.now().toString(), "09:00 AM - 04:00 PM"));
        assertEquals(List.of(Field.VENUE), logged.stream().map(EpmEventUpdate::getField).toList());
    }

    @Test
    void anUpcomingEpmCannotBeMovedToADateThatHasPassed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.update(7L, request("Pragati Maidan", LocalDate.now().minusDays(3).toString(), "09:00 AM - 04:00 PM")));
        assertTrue(e.getMessage().contains("already passed"), e.getMessage());
        verify(eventRepository, never()).save(any(EpmEvent.class));
    }

    private static EpmEventRequestDto request(String venue, String date, String time) {
        EpmEventRequestDto dto = new EpmEventRequestDto();
        dto.setTitle("Export Promotion Meeting");
        dto.setCategory("EPM Meeting");
        dto.setState("Delhi");
        dto.setDistrict("New Delhi");
        dto.setCity("New Delhi");
        dto.setVenue(venue);
        dto.setEventDate(date);
        dto.setTimeRange(time);
        return dto;
    }
}
