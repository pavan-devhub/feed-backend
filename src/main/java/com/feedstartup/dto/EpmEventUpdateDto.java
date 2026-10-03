package com.feedstartup.dto;

import com.feedstartup.model.EpmEventUpdate;

import java.time.LocalDateTime;
import java.util.Locale;

/** One logged change to an EPM. {@code field} is the lower-case EpmEventUpdate.Field name, e.g. "venue". */
public record EpmEventUpdateDto(String field, String oldValue, String newValue, LocalDateTime createdAt) {

    public static EpmEventUpdateDto from(EpmEventUpdate u) {
        return new EpmEventUpdateDto(u.getField().name().toLowerCase(Locale.ROOT), u.getOldValue(), u.getNewValue(), u.getCreatedAt());
    }
}
