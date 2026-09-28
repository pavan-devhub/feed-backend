package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmCategoryService;
import com.feedstartup.service.EpmEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class EpmEventServiceImpl implements EpmEventService {

    private final EpmEventRepository epmEventRepository;
    private final EpmRegistrationRepository epmRegistrationRepository;
    private final EpmVolunteerRepository epmVolunteerRepository;
    private final EpmCategoryService epmCategoryService;

    @Autowired
    public EpmEventServiceImpl(EpmEventRepository epmEventRepository,
                                EpmRegistrationRepository epmRegistrationRepository,
                                EpmVolunteerRepository epmVolunteerRepository,
                                EpmCategoryService epmCategoryService) {
        this.epmEventRepository = epmEventRepository;
        this.epmRegistrationRepository = epmRegistrationRepository;
        this.epmVolunteerRepository = epmVolunteerRepository;
        this.epmCategoryService = epmCategoryService;
    }

    @Override
    public List<EpmEventDto> list(String status, String state, String district, String city, String category, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        List<EpmEvent> events = switch (normalizeStatus(status)) {
            case "previous" -> epmEventRepository.findByCancelledFalseAndEventDateLessThanOrderByEventDateDesc(today);
            case "all" -> epmEventRepository.findByCancelledFalseOrderByEventDateDesc();
            default -> epmEventRepository.findByCancelledFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(today);
        };
        return withCounts(events.stream()
                .filter(filters(null, state, district, city, category, month, year))
                .collect(Collectors.toList()));
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
                .collect(Collectors.toList()));
    }

    @Override
    public EpmEventDto getById(Long id) {
        return EpmEventDto.from(findOrThrow(id));
    }

    @Override
    public EpmEventDto create(EpmEventRequestDto dto) {
        EpmEvent event = new EpmEvent();
        applyRequest(event, dto);
        return EpmEventDto.from(epmEventRepository.save(event));
    }

    @Override
    public EpmEventDto update(Long id, EpmEventRequestDto dto) {
        EpmEvent event = findOrThrow(id);
        applyRequest(event, dto);
        return EpmEventDto.from(epmEventRepository.save(event));
    }

    @Override
    public void delete(Long id) {
        EpmEvent event = findOrThrow(id);
        // Registrations/volunteers are kept: they carry their own copy of the event's city/state/date.
        epmEventRepository.delete(event);
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

    /** DTOs for {@code events}, each with its registration and volunteer counts (two grouped queries). */
    private List<EpmEventDto> withCounts(List<EpmEvent> events) {
        if (events.isEmpty()) return List.of();
        Map<Long, Long> registrations = toCountMap(epmRegistrationRepository.countPerEvent());
        Map<Long, Long> volunteers = toCountMap(epmVolunteerRepository.countPerEvent());
        return events.stream()
                .map(e -> EpmEventDto.from(e).withCounts(
                        registrations.getOrDefault(e.getId(), 0L), volunteers.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
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
