package com.feedstartup.service.impl;

import com.feedstartup.model.EpmAdminActivity;
import com.feedstartup.model.EpmAdminActivity.Action;
import com.feedstartup.model.EpmAdminActivity.Field;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.SystemAdmin;
import com.feedstartup.repository.EpmAdminActivityRepository;
import com.feedstartup.service.SystemAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** The EPM Activity log records who did what to each EPM, one entry per changed detail. */
class EpmAdminActivityServiceImplTest {

    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 10, 15);

    private final EpmAdminActivityRepository activityRepository = mock(EpmAdminActivityRepository.class);
    private final SystemAdminService systemAdminService = mock(SystemAdminService.class);
    private final EpmAdminActivityServiceImpl service = new EpmAdminActivityServiceImpl(activityRepository, systemAdminService);

    private final EpmEvent event = new EpmEvent();
    private final List<EpmAdminActivity> saved = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SystemAdmin ravi = new SystemAdmin();
        ravi.setId(3L);
        ravi.setUsername("ravi");
        when(systemAdminService.get(3L)).thenReturn(ravi);
        when(activityRepository.saveAll(any())).thenAnswer(inv -> {
            ((Iterable<EpmAdminActivity>) inv.getArgument(0)).forEach(saved::add);
            return inv.getArgument(0);
        });

        event.setId(7L);
        event.setTitle("Export Promotion Meeting");
        event.setCategory("Seminar");
        event.setState("Delhi");
        event.setDistrict("New Delhi");
        event.setCity("New Delhi");
        event.setVenue("Pragati Maidan");
        event.setEventDate(EVENT_DATE);
        event.setTimeRange("09:00 AM - 04:00 PM");
    }

    @Test
    void creatingAnEpmRecordsTheAdminAndTheEpmsDateAndPlace() {
        service.recordCreated(event, 3L);

        EpmAdminActivity entry = saved.get(0);
        assertEquals(Action.CREATED, entry.getAction());
        assertEquals(3L, entry.getAdminId());
        assertEquals("ravi", entry.getAdminUsername());
        assertEquals(7L, entry.getEpmEventId());
        assertEquals("Export Promotion Meeting", entry.getEventTitle());
        assertEquals(EVENT_DATE, entry.getEventDate());
        assertEquals("New Delhi", entry.getEventCity());
        assertNull(entry.getField());
    }

    @Test
    void anEditLogsEachChangedDetailTogetherAndInFormOrder() {
        Map<Field, String> before = service.snapshot(event);
        event.setVenue("Bharat Mandapam");
        event.setEventDate(EVENT_DATE.plusDays(5));
        event.setTimeRange(null);

        service.recordEdit(event, before, false, 3L);

        assertEquals(List.of(Field.DATE, Field.TIME, Field.VENUE), saved.stream().map(EpmAdminActivity::getField).toList());
        assertTrue(saved.stream().allMatch(a -> a.getAction() == Action.UPDATED && "ravi".equals(a.getAdminUsername())));
        assertEquals("2026-10-15", saved.get(0).getOldValue());
        assertEquals("2026-10-20", saved.get(0).getNewValue());
        assertEquals("09:00 AM - 04:00 PM", saved.get(1).getOldValue());
        assertNull(saved.get(1).getNewValue());
        // One save, one timestamp - so the log lists them together.
        assertEquals(1, saved.stream().map(EpmAdminActivity::getCreatedAt).distinct().count());
        // The entry shows the EPM as it now stands.
        assertEquals(EVENT_DATE.plusDays(5), saved.get(0).getEventDate());
    }

    @Test
    void anEditThatCancelsTheEpmLogsTheCancellationToo() {
        Map<Field, String> before = service.snapshot(event);
        event.setCancelled(true);

        service.recordEdit(event, before, false, 3L);

        assertEquals(1, saved.size());
        assertEquals(Action.CANCELLED, saved.get(0).getAction());
    }

    @Test
    void anEditThatChangesNothingLogsNothing() {
        service.recordEdit(event, service.snapshot(event), false, 3L);

        verify(activityRepository, never()).saveAll(any());
    }

    @Test
    void aCancellationKeepsTheReason() {
        service.recordCancelled(event, "Heavy rain forecast", 3L);

        assertEquals(Action.CANCELLED, saved.get(0).getAction());
        assertEquals("Heavy rain forecast", saved.get(0).getNewValue());
    }

    @Test
    void theLogIsFilteredByActionAndRejectsAnUnknownOne() {
        when(activityRepository.search(isNull(), any(), isNull(), any())).thenReturn(new PageImpl<>(List.of()));

        service.page(null, "Reinstated", null, 0, 20);
        verify(activityRepository).search(isNull(), eq(Action.REINSTATED), isNull(), any());

        assertThrows(IllegalArgumentException.class, () -> service.page(null, "edited", null, 0, 20));
    }
}
