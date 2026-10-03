package com.feedstartup.service;

import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmEventUpdate.Field;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Works out what an admin edit changed on an EPM, and what has changed since it was first scheduled. */
public final class EpmEventChanges {

    /** The fields whose edits are logged, in the order they're reported. */
    private static final List<Field> TRACKED =
            List.of(Field.DATE, Field.TIME, Field.STATE, Field.DISTRICT, Field.CITY, Field.VENUE, Field.DESCRIPTION);

    private EpmEventChanges() {}

    /** The tracked fields' current values, as EpmEventUpdate stores them. */
    public static Map<Field, String> snapshot(EpmEvent e) {
        Map<Field, String> values = new EnumMap<>(Field.class);
        values.put(Field.DATE, e.getEventDate() == null ? null : e.getEventDate().toString());
        values.put(Field.TIME, e.getTimeRange());
        values.put(Field.STATE, e.getState());
        values.put(Field.DISTRICT, e.getDistrict());
        values.put(Field.CITY, e.getCity());
        values.put(Field.VENUE, e.getVenue());
        values.put(Field.DESCRIPTION, e.getDescription());
        return values;
    }

    /** One update for each tracked field that differs between {@code before} and {@code after}. */
    public static List<EpmEventUpdate> diff(Long eventId, Map<Field, String> before, Map<Field, String> after) {
        return TRACKED.stream()
                .filter(f -> !same(f, before.get(f), after.get(f)))
                .map(f -> new EpmEventUpdate(eventId, f, before.get(f), after.get(f)))
                .toList();
    }

    /**
     * The tracked fields (lower-case names, e.g. "venue") whose current value differs from the one
     * before their first logged change - so a venue that was changed and then changed back no
     * longer counts. {@code history} must be oldest first.
     */
    public static List<String> netChanges(EpmEvent e, List<EpmEventUpdate> history) {
        Map<Field, String> current = snapshot(e);
        Map<Field, String> original = new EnumMap<>(Field.class);
        for (EpmEventUpdate u : history) {
            // containsKey, not putIfAbsent: a field that started out unset has a null original.
            if (TRACKED.contains(u.getField()) && !original.containsKey(u.getField())) {
                original.put(u.getField(), u.getOldValue());
            }
        }
        return TRACKED.stream()
                .filter(original::containsKey)
                .filter(f -> !same(f, original.get(f), current.get(f)))
                .map(f -> f.name().toLowerCase(Locale.ROOT))
                .toList();
    }

    // A place name typed in a different case is the same place; anything else must match exactly.
    private static boolean same(Field field, String a, String b) {
        String x = blankToNull(a);
        String y = blankToNull(b);
        if (x == null || y == null) return x == y;
        return switch (field) {
            case STATE, DISTRICT, CITY, VENUE -> x.equalsIgnoreCase(y);
            default -> x.equals(y);
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
