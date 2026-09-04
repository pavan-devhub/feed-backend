package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmCategory;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.service.EpmEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class EpmEventServiceImpl implements EpmEventService {

    private final EpmEventRepository epmEventRepository;
    private final EpmRegistrationRepository epmRegistrationRepository;

    @Autowired
    public EpmEventServiceImpl(EpmEventRepository epmEventRepository,
                                EpmRegistrationRepository epmRegistrationRepository) {
        this.epmEventRepository = epmEventRepository;
        this.epmRegistrationRepository = epmRegistrationRepository;
    }

    @Override
    public List<EpmEventDto> list(String status, String state, String district, String city, String category, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        String normalizedStatus = (status == null || status.isBlank()) ? "upcoming" : status.trim().toLowerCase();

        List<EpmEvent> events = switch (normalizedStatus) {
            case "previous" -> epmEventRepository.findByCancelledFalseAndEventDateLessThanOrderByEventDateDesc(today);
            case "all" -> epmEventRepository.findByCancelledFalseOrderByEventDateDesc();
            case "upcoming" -> epmEventRepository.findByCancelledFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(today);
            default -> throw new IllegalArgumentException("Invalid status filter: " + status);
        };

        Predicate<EpmEvent> matches = e -> true;
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

        return events.stream()
                .filter(matches)
                .map(EpmEventDto::from)
                .collect(Collectors.toList());
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
        long upcoming = epmEventRepository.findByCancelledFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(today).size();
        return new EpmStatsDto(conducted, districts, participants, upcoming);
    }

    @Override
    public List<EpmCategoryDto> listCategories() {
        return Arrays.stream(EpmCategory.values()).map(EpmCategoryDto::from).collect(Collectors.toList());
    }

    private void applyRequest(EpmEvent event, EpmEventRequestDto dto) {
        event.setTitle(dto.getTitle());
        event.setCategory(resolveCategory(dto.getCategory()));
        event.setState(dto.getState());
        event.setDistrict(dto.getDistrict());
        event.setCity(dto.getCity());
        event.setVenue(dto.getVenue());
        event.setEventDate(parseDate(dto.getEventDate()));
        event.setTimeRange(dto.getTimeRange());
        if (dto.getCancelled() != null) {
            event.setCancelled(dto.getCancelled());
        }
    }

    private String resolveCategory(String raw) {
        return EpmCategory.fromId(raw)
                .map(EpmCategory::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category must be one of: " + EpmCategory.allowedIdsJoined()));
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Event date must be a valid date (yyyy-MM-dd)");
        }
    }

    private EpmEvent findOrThrow(Long id) {
        return epmEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EPM event not found: " + id));
    }
}
