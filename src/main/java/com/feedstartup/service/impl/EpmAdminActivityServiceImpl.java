package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmAdminActivityDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.model.EpmAdminActivity;
import com.feedstartup.model.EpmAdminActivity.Action;
import com.feedstartup.model.EpmAdminActivity.Field;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.repository.EpmAdminActivityRepository;
import com.feedstartup.service.EpmAdminActivityService;
import com.feedstartup.service.SystemAdminService;
import com.feedstartup.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class EpmAdminActivityServiceImpl implements EpmAdminActivityService {

    // Newest first; the entries one save wrote share a timestamp, and then keep Field order.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final EpmAdminActivityRepository activityRepository;
    private final SystemAdminService systemAdminService;

    public EpmAdminActivityServiceImpl(EpmAdminActivityRepository activityRepository, SystemAdminService systemAdminService) {
        this.activityRepository = activityRepository;
        this.systemAdminService = systemAdminService;
    }

    @Override
    public void recordCreated(EpmEvent event, Long adminId) {
        record(event, adminId, Action.CREATED, null, null);
    }

    @Override
    public Map<Field, String> snapshot(EpmEvent e) {
        Map<Field, String> values = new EnumMap<>(Field.class);
        values.put(Field.TITLE, e.getTitle());
        values.put(Field.CATEGORY, e.getCategory());
        values.put(Field.DATE, e.getEventDate() == null ? null : e.getEventDate().toString());
        values.put(Field.TIME, e.getTimeRange());
        values.put(Field.STATE, e.getState());
        values.put(Field.DISTRICT, e.getDistrict());
        values.put(Field.CITY, e.getCity());
        values.put(Field.VENUE, e.getVenue());
        values.put(Field.DESCRIPTION, e.getDescription());
        return values;
    }

    @Override
    public void recordEdit(EpmEvent event, Map<Field, String> before, boolean wasCancelled, Long adminId) {
        LocalDateTime at = LocalDateTime.now();
        String username = usernameOf(adminId);
        Map<Field, String> after = snapshot(event);
        List<EpmAdminActivity> entries = new ArrayList<>();
        // Exact comparison, unlike EpmEventChanges: this is a record of what the admin did, so
        // retyping a venue in another case is logged too.
        for (Field field : Field.values()) {
            if (!Objects.equals(before.get(field), after.get(field))) {
                entries.add(new EpmAdminActivity(at, adminId, username, Action.UPDATED, field, event, before.get(field), after.get(field)));
            }
        }
        if (event.isCancelled() != wasCancelled) {
            entries.add(new EpmAdminActivity(at, adminId, username, event.isCancelled() ? Action.CANCELLED : Action.REINSTATED,
                    null, event, null, null));
        }
        if (!entries.isEmpty()) {
            activityRepository.saveAll(entries);
        }
    }

    @Override
    public void recordCancelled(EpmEvent event, String reason, Long adminId) {
        record(event, adminId, Action.CANCELLED, null, reason);
    }

    @Override
    public void recordReinstated(EpmEvent event, Long adminId) {
        record(event, adminId, Action.REINSTATED, null, null);
    }

    @Override
    public void recordDeleted(EpmEvent event, Long adminId) {
        record(event, adminId, Action.DELETED, null, null);
    }

    @Override
    public PageDto<EpmAdminActivityDto> page(Long adminId, String action, LocalDate eventDate, int page, int size) {
        return PageDto.of(activityRepository.search(adminId, parseAction(action), eventDate, Paging.of(page, size, NEWEST_FIRST)),
                EpmAdminActivityDto::from);
    }

    @Override
    public List<EpmAdminActivityDto.Admin> admins() {
        return activityRepository.findAdmins().stream()
                .map(row -> new EpmAdminActivityDto.Admin(((Number) row[0]).longValue(), (String) row[1]))
                .sorted(Comparator.comparing(EpmAdminActivityDto.Admin::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void record(EpmEvent event, Long adminId, Action action, String oldValue, String newValue) {
        activityRepository.saveAll(List.of(
                new EpmAdminActivity(LocalDateTime.now(), adminId, usernameOf(adminId), action, null, event, oldValue, newValue)));
    }

    private String usernameOf(Long adminId) {
        return systemAdminService.get(adminId).getUsername();
    }

    private static Action parseAction(String action) {
        if (action == null || action.isBlank()) return null;
        try {
            return Action.valueOf(action.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action \"" + action + "\" - use created, updated, cancelled, reinstated or deleted");
        }
    }
}
