package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventFacetsDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmEventUpdateRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmAdminActivityService;
import com.feedstartup.service.EpmCategoryService;
import com.feedstartup.service.EpmEventFilter;
import com.feedstartup.service.EpmEventFilter.Status;
import com.feedstartup.service.EpmVenueService;
import com.feedstartup.util.Paging;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The EPM lists come a page at a time from the database, with their filters' choices on the side. */
class EpmEventServiceImplPagingTest {

    private final EpmEventRepository eventRepository = mock(EpmEventRepository.class);
    private final EpmRegistrationRepository registrationRepository = mock(EpmRegistrationRepository.class);
    private final EpmEventServiceImpl service = new EpmEventServiceImpl(eventRepository, registrationRepository,
            mock(EpmVolunteerRepository.class), mock(EpmCategoryService.class), mock(EpmVenueService.class),
            mock(EpmEventUpdateRepository.class), mock(EpmAdminActivityService.class), mock(ApplicationEventPublisher.class));

    @Test
    void aPageCountsSignUpsForItsOwnEventsOnly() {
        EpmEvent first = event(1L, "Telangana", "Warangal", "Hanamkonda", "Seminar");
        EpmEvent second = event(2L, "Telangana", "Hyderabad", "Madhapur", "Seminar");
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 2), 7));
        when(registrationRepository.countPerEvent(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{2L, 4L}));

        PageDto<EpmEventDto> page = service.page(EpmEventFilter.forAdmin("upcoming", null, null, null, null, null, null, null), 1, 2);

        assertEquals(List.of(1L, 2L), page.items().stream().map(EpmEventDto::getId).toList());
        assertEquals(List.of(0L, 4L), page.items().stream().map(EpmEventDto::getRegistrationCount).toList());
        assertEquals(7, page.total());
        assertEquals(4, page.totalPages());
        verify(registrationRepository).countPerEvent(List.of(1L, 2L));
    }

    @Test
    void anOversizedPageIsCappedAndUpcomingEventsComeSoonestFirst() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(eventRepository.findAll(any(Specification.class), pageable.capture())).thenReturn(new PageImpl<>(List.of()));

        service.page(EpmEventFilter.forPublic("upcoming", null, null, null, null, null, null, null, false), -3, 5000);

        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(Paging.MAX_PAGE_SIZE, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("id")), pageable.getValue().getSort());
    }

    @Test
    void facetsListEachPlaceOnceSortedWithEveryCategorysCount() {
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(
                event(1L, "Telangana", "Warangal", "Hanamkonda", "Seminar"),
                event(2L, "Andhra Pradesh", "Krishna", "Vijayawada", "Buyer-Seller Meet"),
                event(3L, "Telangana", "Warangal", "Hanamkonda", "Seminar"),
                event(4L, "Telangana", "Hyderabad", "Madhapur", null)));

        EpmEventFacetsDto facets = service.facets(EpmEventFilter.forPublic("previous", "ignored", null, null, null, null, null, null, false));

        assertEquals(4, facets.total());
        assertEquals(List.of(
                new EpmEventFacetsDto.Place("Andhra Pradesh", "Krishna", "Vijayawada"),
                new EpmEventFacetsDto.Place("Telangana", "Hyderabad", "Madhapur"),
                new EpmEventFacetsDto.Place("Telangana", "Warangal", "Hanamkonda")), facets.places());
        assertEquals(Map.of("Seminar", 2L, "Buyer-Seller Meet", 1L), facets.categoryCounts());
    }

    @Test
    void publicListsShowCancelledEventsOnlyAmongUpcomingOnesAndOnlyWhenAsked() {
        assertTrue(EpmEventFilter.forPublic("upcoming", null, null, null, null, null, null, null, true).includeCancelled());
        assertFalse(EpmEventFilter.forPublic("previous", null, null, null, null, null, null, null, true).includeCancelled());
        assertFalse(EpmEventFilter.forPublic("upcoming", null, null, null, null, null, null, null, false).includeCancelled());
        assertTrue(EpmEventFilter.forAdmin("previous", null, null, null, null, null, null, null).includeCancelled());
    }

    @Test
    void statusDefaultsToUpcomingAndRejectsAnythingElseUnknown() {
        assertEquals(Status.UPCOMING, Status.parse(null));
        assertEquals(Status.PREVIOUS, Status.parse(" Previous "));
        assertThrows(IllegalArgumentException.class, () -> Status.parse("soon"));
        assertThrows(IllegalArgumentException.class,
                () -> EpmEventFilter.forAdmin("all", null, null, null, null, null, 13, null));
    }

    private static EpmEvent event(Long id, String state, String district, String city, String category) {
        EpmEvent e = new EpmEvent();
        e.setId(id);
        e.setTitle("EPM " + id);
        e.setState(state);
        e.setDistrict(district);
        e.setCity(city);
        e.setVenue("Town Hall");
        e.setCategory(category);
        e.setEventDate(LocalDate.now().plusDays(id));
        return e;
    }
}
