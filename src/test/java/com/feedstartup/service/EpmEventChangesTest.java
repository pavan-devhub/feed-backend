package com.feedstartup.service;

import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmEventUpdate.Field;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What an admin edit logs, and what an EPM's status says changed since it was scheduled. */
class EpmEventChangesTest {

    @Test
    void anEditLogsOnlyTheFieldsThatReallyChanged() {
        EpmEvent event = event("2026-10-15", "10:00 AM - 02:00 PM", "Pragati Maidan", null);
        Map<Field, String> before = EpmEventChanges.snapshot(event);
        event.setEventDate(LocalDate.parse("2026-10-20"));
        event.setVenue("PRAGATI MAIDAN");            // same place, different case - not a change
        event.setDescription("Bring a valid ID");    // was unset

        List<EpmEventUpdate> updates = EpmEventChanges.diff(7L, before, EpmEventChanges.snapshot(event));

        assertEquals(List.of(Field.DATE, Field.DESCRIPTION), updates.stream().map(EpmEventUpdate::getField).toList());
        assertEquals("2026-10-15", updates.get(0).getOldValue());
        assertEquals("2026-10-20", updates.get(0).getNewValue());
        assertNull(updates.get(1).getOldValue());
        assertEquals(7L, updates.get(1).getEpmEventId());
    }

    @Test
    void anUneditedEventHasNoChanges() {
        EpmEvent event = event("2026-10-15", null, "Pragati Maidan", null);
        assertTrue(EpmEventChanges.diff(7L, EpmEventChanges.snapshot(event), EpmEventChanges.snapshot(event)).isEmpty());
        assertTrue(EpmEventChanges.netChanges(event, List.of()).isEmpty());
    }

    @Test
    void theStatusComparesWithTheOriginalValueSoAChangeThatWasUndoneDropsOut() {
        EpmEvent event = event("2026-10-20", "10:00 AM - 05:00 PM", "Pragati Maidan", null);
        List<EpmEventUpdate> history = List.of(
                new EpmEventUpdate(7L, Field.VENUE, "Pragati Maidan", "Bharat Mandapam"),
                new EpmEventUpdate(7L, Field.DATE, "2026-10-15", "2026-10-20"),
                new EpmEventUpdate(7L, Field.TIME, null, "10:00 AM - 05:00 PM"),
                new EpmEventUpdate(7L, Field.VENUE, "Bharat Mandapam", "Pragati Maidan"),
                new EpmEventUpdate(7L, Field.CANCELLED, null, "Heavy rain"),
                new EpmEventUpdate(7L, Field.RESTORED, null, null));

        assertEquals(List.of("date", "time"), EpmEventChanges.netChanges(event, history));
    }

    private static EpmEvent event(String date, String time, String venue, String description) {
        EpmEvent e = new EpmEvent();
        e.setId(7L);
        e.setTitle("Export Promotion Meeting");
        e.setState("Delhi");
        e.setDistrict("New Delhi");
        e.setCity("New Delhi");
        e.setVenue(venue);
        e.setEventDate(LocalDate.parse(date));
        e.setTimeRange(time);
        e.setDescription(description);
        return e;
    }
}
