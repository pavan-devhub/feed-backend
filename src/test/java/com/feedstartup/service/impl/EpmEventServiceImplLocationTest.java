package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.EpmVenueDto;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmAdminActivityService;
import com.feedstartup.service.EpmCategoryService;
import com.feedstartup.service.EpmVenueService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The admin EPM form's place suggestions, and new places landing in the venue list on save. */
class EpmEventServiceImplLocationTest {

    private final EpmEventRepository eventRepository = mock(EpmEventRepository.class);
    private final EpmCategoryService categoryService = mock(EpmCategoryService.class);
    private final EpmVenueService venueService = mock(EpmVenueService.class);
    private final EpmEventServiceImpl service = new EpmEventServiceImpl(eventRepository,
            mock(EpmRegistrationRepository.class), mock(EpmVolunteerRepository.class), categoryService, venueService,
            mock(EpmEventUpdateRepository.class), mock(EpmAdminActivityService.class), mock(ApplicationEventPublisher.class));

    @BeforeEach
    void setUp() {
        when(categoryService.resolveName(any())).thenReturn("Seminar");
        when(eventRepository.save(any(EpmEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void suggestsEveryPlaceFromEventsAndTheVenueListSortedWithCaseOnlyDuplicatesMerged() {
        when(venueService.list()).thenReturn(List.of(
                new EpmVenueDto(1L, "Convention Centre", "Telangana", "Hyderabad", "Madhapur", null)));
        when(eventRepository.findDistinctLocations()).thenReturn(List.of(
                new EpmLocationDto("telangana", "hyderabad", "madhapur", "convention centre"),
                new EpmLocationDto("Andhra Pradesh", "Krishna", "Vijayawada", "Town Hall")));

        assertEquals(List.of(
                new EpmLocationDto("Andhra Pradesh", "Krishna", "Vijayawada", "Town Hall"),
                new EpmLocationDto("Telangana", "Hyderabad", "Madhapur", "Convention Centre")),
                service.listLocations());
    }

    @Test
    void creatingAnEventRecordsItsPlaceInTheVenueList() {
        service.create(request(" Telangana ", "Warangal", "Hanamkonda", "Kakatiya Hall"), 1L);

        verify(venueService).recordIfNew(new EpmLocationDto("Telangana", "Warangal", "Hanamkonda", "Kakatiya Hall"));
    }

    @Test
    void editingAnEventRecordsItsPlaceOnlyWhenThePlaceChanged() {
        EpmEvent existing = event("Telangana", "Warangal", "Hanamkonda", "Kakatiya Hall");
        when(eventRepository.findById(7L)).thenReturn(Optional.of(existing));

        service.update(7L, request("Telangana", "Warangal", "Hanamkonda", "KAKATIYA HALL"), 1L);
        verify(venueService, never()).recordIfNew(any());

        service.update(7L, request("Telangana", "Warangal", "Hanamkonda", "New Hall"), 1L);
        verify(venueService).recordIfNew(new EpmLocationDto("Telangana", "Warangal", "Hanamkonda", "New Hall"));
    }

    private static EpmEventRequestDto request(String state, String district, String city, String venue) {
        EpmEventRequestDto dto = new EpmEventRequestDto();
        dto.setTitle("Buyer-Seller Meet");
        dto.setCategory("Seminar");
        dto.setState(state);
        dto.setDistrict(district);
        dto.setCity(city);
        dto.setVenue(venue);
        dto.setEventDate("2026-11-15");
        return dto;
    }

    private static EpmEvent event(String state, String district, String city, String venue) {
        EpmEvent e = new EpmEvent();
        e.setId(7L);
        e.setTitle("Buyer-Seller Meet");
        e.setState(state);
        e.setDistrict(district);
        e.setCity(city);
        e.setVenue(venue);
        return e;
    }
}
