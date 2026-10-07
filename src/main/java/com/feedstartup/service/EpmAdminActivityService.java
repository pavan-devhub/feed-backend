package com.feedstartup.service;

import com.feedstartup.dto.EpmAdminActivityDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.model.EpmAdminActivity.Field;
import com.feedstartup.model.EpmEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The admin panel's EPM Activity log: which admin scheduled, changed, cancelled, reinstated or
 * deleted each EPM. EpmEventServiceImpl records every admin action in the same transaction as the
 * action itself. Entries can only be added and read - see EpmAdminActivity.
 */
public interface EpmAdminActivityService {

    void recordCreated(EpmEvent event, Long adminId);

    /** The EPM's editable details, to hand to {@link #recordEdit} - take it before applying the edit. */
    Map<Field, String> snapshot(EpmEvent event);

    /**
     * One entry per detail the edit changed (compared with {@code before}, from {@link #snapshot}),
     * plus a cancellation or reinstatement if the edit flipped {@code cancelled}. Nothing if the
     * edit changed nothing.
     */
    void recordEdit(EpmEvent event, Map<Field, String> before, boolean wasCancelled, Long adminId);

    void recordCancelled(EpmEvent event, String reason, Long adminId);

    void recordReinstated(EpmEvent event, Long adminId);

    /** Call before the EPM is deleted. */
    void recordDeleted(EpmEvent event, Long adminId);

    /**
     * A page of entries, newest first - the entries one save wrote stay together, in Field order.
     * Every filter is optional: {@code action} is a lower-case Action name such as "cancelled",
     * {@code eventDate} the EPM's date. {@code page} is 0-based.
     */
    PageDto<EpmAdminActivityDto> page(Long adminId, String action, LocalDate eventDate, int page, int size);

    /** Every admin with an entry - including ones since deleted - by username. */
    List<EpmAdminActivityDto.Admin> admins();
}
