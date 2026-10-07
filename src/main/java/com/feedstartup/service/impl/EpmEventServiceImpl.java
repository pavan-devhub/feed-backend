package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventFacetsDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmEventUpdateDto;
import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmAdminActivity;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmEventUpdate.Field;
import com.feedstartup.realtime.LiveUpdateEvents;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmAdminActivityService;
import com.feedstartup.service.EpmCategoryService;
import com.feedstartup.service.EpmEventChanges;
import com.feedstartup.service.EpmEventFilter;
import com.feedstartup.service.EpmEventService;
import com.feedstartup.service.EpmVenueService;
import com.feedstartup.util.Paging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EpmEventServiceImpl implements EpmEventService {

    private final EpmEventRepository epmEventRepository;
    private final EpmRegistrationRepository epmRegistrationRepository;
    private final EpmVolunteerRepository epmVolunteerRepository;
    private final EpmCategoryService epmCategoryService;
    private final EpmVenueService epmVenueService;
    private final EpmEventUpdateRepository epmEventUpdateRepository;
    private final EpmAdminActivityService epmAdminActivityService;
    // Every change is announced as LiveUpdateEvents.EpmChanged, so open EPM lists, bells and
    // Status of Activities pages reload (see LiveUpdateBroadcaster).
    private final ApplicationEventPublisher events;

    // "30 Sep 2026", as the admin panel shows dates.
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private static final Comparator<EpmEventFacetsDto.Place> PLACE_ORDER =
            Comparator.comparing(EpmEventFacetsDto.Place::state, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(EpmEventFacetsDto.Place::district, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(EpmEventFacetsDto.Place::city, String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<EpmLocationDto> LOCATION_ORDER =
            Comparator.comparing(EpmLocationDto::state, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(EpmLocationDto::district, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(EpmLocationDto::city, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(EpmLocationDto::venue, String.CASE_INSENSITIVE_ORDER);

    @Autowired
    public EpmEventServiceImpl(EpmEventRepository epmEventRepository,
                                EpmRegistrationRepository epmRegistrationRepository,
                                EpmVolunteerRepository epmVolunteerRepository,
                                EpmCategoryService epmCategoryService,
                                EpmVenueService epmVenueService,
                                EpmEventUpdateRepository epmEventUpdateRepository,
                                EpmAdminActivityService epmAdminActivityService,
                                ApplicationEventPublisher events) {
        this.epmEventRepository = epmEventRepository;
        this.epmRegistrationRepository = epmRegistrationRepository;
        this.epmVolunteerRepository = epmVolunteerRepository;
        this.epmCategoryService = epmCategoryService;
        this.epmVenueService = epmVenueService;
        this.epmEventUpdateRepository = epmEventUpdateRepository;
        this.epmAdminActivityService = epmAdminActivityService;
        this.events = events;
    }

    @Override
    public List<EpmEventDto> list(EpmEventFilter filter) {
        return withCounts(epmEventRepository.findAll(filter.toSpecification(LocalDate.now()), filter.sort()), true);
    }

    @Override
    public PageDto<EpmEventDto> page(EpmEventFilter filter, int page, int size) {
        Page<EpmEvent> rows = epmEventRepository.findAll(filter.toSpecification(LocalDate.now()),
                Paging.of(page, size, filter.sort()));
        // Counts and change history are looked up for this page's EPMs only.
        return PageDto.of(rows, withCounts(rows.getContent(), true));
    }

    @Override
    public EpmEventFacetsDto facets(EpmEventFilter filter) {
        // A tab holds at most a few hundred EPMs (each is a meeting held somewhere), so its places
        // and category counts are worked out from the rows rather than with one query apiece.
        List<EpmEvent> events = epmEventRepository.findAll(filter.statusOnly().toSpecification(LocalDate.now()));
        List<EpmEventFacetsDto.Place> places = events.stream()
                .map(e -> new EpmEventFacetsDto.Place(e.getState(), e.getDistrict(), e.getCity()))
                .distinct()
                .sorted(PLACE_ORDER)
                .toList();
        Map<String, Long> categoryCounts = events.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(EpmEvent::getCategory, TreeMap::new, Collectors.counting()));
        return new EpmEventFacetsDto(events.size(), places, categoryCounts);
    }

    @Override
    public EpmEventDto getById(Long id) {
        return withCounts(List.of(findOrThrow(id)), true).get(0);
    }

    @Override
    @Transactional
    public EpmEventDto create(EpmEventRequestDto dto, Long adminId) {
        EpmEvent event = new EpmEvent();
        applyRequest(event, dto);
        EpmEvent saved = epmEventRepository.save(event);
        epmVenueService.recordIfNew(locationOf(saved));
        epmAdminActivityService.recordCreated(saved, adminId);
        events.publishEvent(new LiveUpdateEvents.EpmChanged(saved.getId()));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public EpmEventDto update(Long id, EpmEventRequestDto dto, Long adminId) {
        EpmEvent event = findOrThrow(id);
        requireUpcoming(event, "edited");
        if (parseDate(dto.getEventDate()).isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("An upcoming EPM can't be moved to a date that has already passed - pick today or a later date");
        }
        Map<Field, String> before = EpmEventChanges.snapshot(event);
        Map<EpmAdminActivity.Field, String> adminBefore = epmAdminActivityService.snapshot(event);
        boolean wasCancelled = event.isCancelled();
        String locationBefore = locationKey(locationOf(event));
        applyRequest(event, dto);
        EpmEvent saved = epmEventRepository.save(event);

        List<EpmEventUpdate> changes = EpmEventChanges.diff(saved.getId(), before, EpmEventChanges.snapshot(saved));
        epmEventUpdateRepository.saveAll(changes);
        if (saved.isCancelled() != wasCancelled) {
            epmEventUpdateRepository.save(new EpmEventUpdate(saved.getId(), saved.isCancelled() ? Field.CANCELLED : Field.RESTORED, null, null));
        }
        // Registrations/volunteers keep a copy of the event's date and place for when the event is
        // later removed - while it exists, that copy follows the event so the admin's "EPMs held on
        // this date" filter still finds the people registered for a rescheduled EPM.
        if (changes.stream().anyMatch(u -> u.getField() == Field.DATE || u.getField() == Field.CITY || u.getField() == Field.STATE)) {
            epmRegistrationRepository.syncEventSnapshot(saved.getId(), saved.getCity(), saved.getState(), saved.getEventDate());
            epmVolunteerRepository.syncEventSnapshot(saved.getId(), saved.getCity(), saved.getState(), saved.getEventDate());
        }
        // Only a changed location goes into the venue list, so editing an old EPM's other details
        // doesn't bring back a venue the admin has since removed from the list.
        if (!locationKey(locationOf(saved)).equals(locationBefore)) {
            epmVenueService.recordIfNew(locationOf(saved));
        }
        epmAdminActivityService.recordEdit(saved, adminBefore, wasCancelled, adminId);
        events.publishEvent(new LiveUpdateEvents.EpmChanged(saved.getId()));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public EpmEventDto cancel(Long id, String reason, Long adminId) {
        EpmEvent event = findOrThrow(id);
        requireUpcoming(event, "cancelled");
        if (event.isCancelled()) {
            throw new ConflictException("\"" + event.getTitle() + "\" is already cancelled");
        }
        event.setCancelled(true);
        EpmEvent saved = epmEventRepository.save(event);
        String note = reason == null || reason.isBlank() ? null : reason.trim();
        epmEventUpdateRepository.save(new EpmEventUpdate(saved.getId(), Field.CANCELLED, null, note));
        epmAdminActivityService.recordCancelled(saved, note, adminId);
        events.publishEvent(new LiveUpdateEvents.EpmChanged(saved.getId()));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public EpmEventDto restore(Long id, Long adminId) {
        EpmEvent event = findOrThrow(id);
        requireUpcoming(event, "reinstated");
        if (!event.isCancelled()) {
            throw new ConflictException("\"" + event.getTitle() + "\" is not cancelled");
        }
        event.setCancelled(false);
        EpmEvent saved = epmEventRepository.save(event);
        epmEventUpdateRepository.save(new EpmEventUpdate(saved.getId(), Field.RESTORED, null, null));
        epmAdminActivityService.recordReinstated(saved, adminId);
        events.publishEvent(new LiveUpdateEvents.EpmChanged(saved.getId()));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, Long adminId) {
        EpmEvent event = findOrThrow(id);
        // Registrations/volunteers are kept: they carry their own copy of the event's city/state/date.
        // So is the EPM Activity log, which copies what it needs and so outlives the EPM.
        epmAdminActivityService.recordDeleted(event, adminId);
        epmEventUpdateRepository.deleteByEpmEventId(event.getId());
        epmEventRepository.delete(event);
        events.publishEvent(new LiveUpdateEvents.EpmChanged(event.getId()));
    }

    @Override
    public Map<Long, EpmEventDto> findWithHistory(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        List<EpmEvent> events = epmEventRepository.findAllById(ids);
        Map<Long, List<EpmEventUpdate>> history = historyFor(events);
        Map<Long, EpmEventDto> result = new HashMap<>();
        for (EpmEvent e : events) {
            result.put(e.getId(), withHistory(EpmEventDto.from(e), e, history.getOrDefault(e.getId(), List.of())));
        }
        return result;
    }

    @Override
    public EpmStatsDto getStats() {
        LocalDate today = LocalDate.now();
        long conducted = epmEventRepository.countByCancelledFalseAndEventDateLessThan(today);
        long districts = epmEventRepository.countDistinctDistricts(today);
        // "Total Attendees" is registrations only - volunteers sign up to help run an EPM, not to
        // attend one, so they're intentionally excluded from this count.
        long participants = epmRegistrationRepository.count();
        long upcoming = epmEventRepository.countByCancelledFalseAndEventDateGreaterThanEqual(today);
        return new EpmStatsDto(conducted, districts, participants, upcoming);
    }

    @Override
    public List<EpmCategoryDto> listCategories() {
        return epmCategoryService.listPublic();
    }

    @Override
    public List<EpmLocationDto> listLocations() {
        // Venue-list spellings go in first, so they win over an EPM's that differs only in case.
        Map<String, EpmLocationDto> distinct = new LinkedHashMap<>();
        Stream.concat(
                        epmVenueService.list().stream()
                                .map(v -> new EpmLocationDto(v.state(), v.district(), v.city(), v.name())),
                        epmEventRepository.findDistinctLocations().stream())
                .forEach(l -> distinct.putIfAbsent(locationKey(l), l));
        return distinct.values().stream().sorted(LOCATION_ORDER).collect(Collectors.toList());
    }

    // --- helpers ---------------------------------------------------------------------------

    /**
     * DTOs for {@code events}, each with its registration and volunteer counts (two grouped
     * queries over just these events) and, if {@code withChanges}, what has changed since it was
     * scheduled and every logged update, newest first (one more).
     */
    private List<EpmEventDto> withCounts(List<EpmEvent> events, boolean withChanges) {
        if (events.isEmpty()) return List.of();
        List<Long> ids = events.stream().map(EpmEvent::getId).toList();
        Map<Long, Long> registrations = toCountMap(epmRegistrationRepository.countPerEvent(ids));
        Map<Long, Long> volunteers = toCountMap(epmVolunteerRepository.countPerEvent(ids));
        Map<Long, List<EpmEventUpdate>> history = withChanges ? historyFor(events) : Map.of();
        return events.stream()
                .map(e -> {
                    EpmEventDto dto = EpmEventDto.from(e).withCounts(
                            registrations.getOrDefault(e.getId(), 0L), volunteers.getOrDefault(e.getId(), 0L));
                    return withChanges ? withHistory(dto, e, history.getOrDefault(e.getId(), List.of())) : dto;
                })
                .collect(Collectors.toList());
    }

    /** {@code log} is oldest first; the DTO lists it newest first. */
    private static EpmEventDto withHistory(EpmEventDto dto, EpmEvent e, List<EpmEventUpdate> log) {
        List<EpmEventUpdateDto> newestFirst = log.stream().map(EpmEventUpdateDto::from).collect(Collectors.toList());
        Collections.reverse(newestFirst);
        return dto.withHistory(EpmEventChanges.netChanges(e, log), newestFirst);
    }

    /**
     * A previous EPM is a record of what happened, so it stays as it was: only upcoming ones (today
     * or later) can be edited, cancelled or reinstated.
     */
    private static void requireUpcoming(EpmEvent e, String action) {
        if (e.getEventDate() != null && e.getEventDate().isBefore(LocalDate.now())) {
            throw new ConflictException("\"" + e.getTitle() + "\" was held on " + DISPLAY_DATE.format(e.getEventDate())
                    + " - previous EPMs can't be " + action);
        }
    }

    /** Each event's update log, oldest first. */
    private Map<Long, List<EpmEventUpdate>> historyFor(List<EpmEvent> events) {
        List<Long> ids = events.stream().map(EpmEvent::getId).toList();
        return epmEventUpdateRepository.findByEpmEventIdInOrderByCreatedAtAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(EpmEventUpdate::getEpmEventId));
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private void applyRequest(EpmEvent event, EpmEventRequestDto dto) {
        event.setTitle(dto.getTitle().trim());
        event.setCategory(epmCategoryService.resolveName(dto.getCategory()));
        event.setState(dto.getState().trim());
        event.setDistrict(dto.getDistrict().trim());
        event.setCity(dto.getCity().trim());
        event.setVenue(dto.getVenue().trim());
        event.setEventDate(parseDate(dto.getEventDate()));
        event.setTimeRange(dto.getTimeRange() == null || dto.getTimeRange().isBlank() ? null : dto.getTimeRange().trim());
        event.setDescription(dto.getDescription() == null || dto.getDescription().isBlank() ? null : dto.getDescription().trim());
        if (dto.getCancelled() != null) {
            event.setCancelled(dto.getCancelled());
        }
    }

    private static EpmLocationDto locationOf(EpmEvent e) {
        return new EpmLocationDto(e.getState(), e.getDistrict(), e.getCity(), e.getVenue());
    }

    private static String locationKey(EpmLocationDto l) {
        return String.join("|", l.state(), l.district(), l.city(), l.venue()).toLowerCase(Locale.ROOT);
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Event date must be a valid date (yyyy-MM-dd)");
        }
    }

    private EpmEvent findOrThrow(Long id) {
        return epmEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EPM event not found: " + id));
    }
}
