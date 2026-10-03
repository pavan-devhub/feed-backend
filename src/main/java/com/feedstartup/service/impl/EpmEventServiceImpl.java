package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmEventUpdateDto;
import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmEventUpdate;
import com.feedstartup.model.EpmEventUpdate.Field;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmCategoryService;
import com.feedstartup.service.EpmEventChanges;
import com.feedstartup.service.EpmEventService;
import com.feedstartup.service.EpmVenueService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.function.Predicate;
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

    // "30 Sep 2026", as the admin panel shows dates.
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

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
                                EpmEventUpdateRepository epmEventUpdateRepository) {
        this.epmEventRepository = epmEventRepository;
        this.epmRegistrationRepository = epmRegistrationRepository;
        this.epmVolunteerRepository = epmVolunteerRepository;
        this.epmCategoryService = epmCategoryService;
        this.epmVenueService = epmVenueService;
        this.epmEventUpdateRepository = epmEventUpdateRepository;
    }

    @Override
    public List<EpmEventDto> list(String status, String state, String district, String city, String category, Integer month,
                                  Integer year, boolean includeCancelled) {
        LocalDate today = LocalDate.now();
        List<EpmEvent> events = switch (normalizeStatus(status)) {
            case "previous" -> epmEventRepository.findByCancelledFalseAndEventDateLessThanOrderByEventDateDesc(today);
            case "all" -> epmEventRepository.findByCancelledFalseOrderByEventDateDesc();
            default -> includeCancelled
                    ? epmEventRepository.findByEventDateGreaterThanEqualOrderByEventDateAsc(today)
                    : epmEventRepository.findByCancelledFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(today);
        };
        // With each EPM's changes, so the register / volunteer lists can show what was rescheduled or moved.
        return withCounts(events.stream()
                .filter(filters(null, state, district, city, category, month, year))
                .collect(Collectors.toList()), true);
    }

    @Override
    public List<EpmEventDto> adminList(String status, String query, String state, String district, String city,
                                       String category, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        List<EpmEvent> events = switch (normalizeStatus(status)) {
            case "previous" -> epmEventRepository.findByEventDateLessThanOrderByEventDateDesc(today);
            case "all" -> epmEventRepository.findAllByOrderByEventDateDesc();
            default -> epmEventRepository.findByEventDateGreaterThanEqualOrderByEventDateAsc(today);
        };
        return withCounts(events.stream()
                .filter(filters(query, state, district, city, category, month, year))
                .collect(Collectors.toList()), true);
    }

    @Override
    public EpmEventDto getById(Long id) {
        return EpmEventDto.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public EpmEventDto create(EpmEventRequestDto dto) {
        EpmEvent event = new EpmEvent();
        applyRequest(event, dto);
        EpmEvent saved = epmEventRepository.save(event);
        epmVenueService.recordIfNew(locationOf(saved));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public EpmEventDto update(Long id, EpmEventRequestDto dto) {
        EpmEvent event = findOrThrow(id);
        requireUpcoming(event, "edited");
        if (parseDate(dto.getEventDate()).isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("An upcoming EPM can't be moved to a date that has already passed - pick today or a later date");
        }
        Map<Field, String> before = EpmEventChanges.snapshot(event);
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
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public EpmEventDto cancel(Long id, String reason) {
        EpmEvent event = findOrThrow(id);
        requireUpcoming(event, "cancelled");
        if (event.isCancelled()) {
            throw new ConflictException("\"" + event.getTitle() + "\" is already cancelled");
        }
        event.setCancelled(true);
        EpmEvent saved = epmEventRepository.save(event);
        String note = reason == null || reason.isBlank() ? null : reason.trim();
        epmEventUpdateRepository.save(new EpmEventUpdate(saved.getId(), Field.CANCELLED, null, note));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public EpmEventDto restore(Long id) {
        EpmEvent event = findOrThrow(id);
        requireUpcoming(event, "reinstated");
        if (!event.isCancelled()) {
            throw new ConflictException("\"" + event.getTitle() + "\" is not cancelled");
        }
        event.setCancelled(false);
        EpmEvent saved = epmEventRepository.save(event);
        epmEventUpdateRepository.save(new EpmEventUpdate(saved.getId(), Field.RESTORED, null, null));
        return EpmEventDto.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EpmEvent event = findOrThrow(id);
        // Registrations/volunteers are kept: they carry their own copy of the event's city/state/date.
        epmEventUpdateRepository.deleteByEpmEventId(event.getId());
        epmEventRepository.delete(event);
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

    private static String normalizeStatus(String status) {
        String normalized = (status == null || status.isBlank()) ? "upcoming" : status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("upcoming", "previous", "all").contains(normalized)) {
            throw new IllegalArgumentException("Invalid status filter: " + status);
        }
        return normalized;
    }

    private static Predicate<EpmEvent> filters(String query, String state, String district, String city,
                                               String category, Integer month, Integer year) {
        Predicate<EpmEvent> matches = e -> true;
        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            matches = matches.and(e -> containsIgnoreCase(e.getTitle(), q) || containsIgnoreCase(e.getCity(), q)
                    || containsIgnoreCase(e.getDistrict(), q) || containsIgnoreCase(e.getState(), q)
                    || containsIgnoreCase(e.getVenue(), q) || containsIgnoreCase(e.getCategory(), q));
        }
        if (state != null && !state.isBlank()) {
            matches = matches.and(e -> e.getState() != null && e.getState().equalsIgnoreCase(state.trim()));
        }
        if (district != null && !district.isBlank()) {
            matches = matches.and(e -> e.getDistrict() != null && e.getDistrict().equalsIgnoreCase(district.trim()));
        }
        if (city != null && !city.isBlank()) {
            matches = matches.and(e -> e.getCity() != null && e.getCity().equalsIgnoreCase(city.trim()));
        }
        if (category != null && !category.isBlank()) {
            matches = matches.and(e -> e.getCategory() != null && e.getCategory().equalsIgnoreCase(category.trim()));
        }
        if (month != null) {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Month must be between 1 and 12");
            }
            matches = matches.and(e -> e.getEventDate() != null && e.getEventDate().getMonthValue() == month);
        }
        if (year != null) {
            matches = matches.and(e -> e.getEventDate() != null && e.getEventDate().getYear() == year);
        }
        return matches;
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    /**
     * DTOs for {@code events}, each with its registration and volunteer counts (two grouped
     * queries) and, if {@code withChanges}, what has changed since it was scheduled and every
     * logged update, newest first (one more).
     */
    private List<EpmEventDto> withCounts(List<EpmEvent> events, boolean withChanges) {
        if (events.isEmpty()) return List.of();
        Map<Long, Long> registrations = toCountMap(epmRegistrationRepository.countPerEvent());
        Map<Long, Long> volunteers = toCountMap(epmVolunteerRepository.countPerEvent());
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
