package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmAdminOverviewDto;
import com.feedstartup.repository.EpmCategoryRepository;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmGalleryImageRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.repository.EpmReviewRepository;
import com.feedstartup.repository.EpmVenueRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmAdminOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EpmAdminOverviewServiceImpl implements EpmAdminOverviewService {

    private final EpmEventRepository eventRepository;
    private final EpmRegistrationRepository registrationRepository;
    private final EpmVolunteerRepository volunteerRepository;
    private final EpmGalleryImageRepository imageRepository;
    private final EpmReviewRepository reviewRepository;
    private final EpmCategoryRepository categoryRepository;
    private final EpmVenueRepository venueRepository;

    @Autowired
    public EpmAdminOverviewServiceImpl(EpmEventRepository eventRepository,
                                       EpmRegistrationRepository registrationRepository,
                                       EpmVolunteerRepository volunteerRepository,
                                       EpmGalleryImageRepository imageRepository,
                                       EpmReviewRepository reviewRepository,
                                       EpmCategoryRepository categoryRepository,
                                       EpmVenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.volunteerRepository = volunteerRepository;
        this.imageRepository = imageRepository;
        this.reviewRepository = reviewRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public EpmAdminOverviewDto getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        return new EpmAdminOverviewDto(
                eventRepository.countByCancelledFalseAndEventDateGreaterThanEqual(today),
                eventRepository.countByCancelledFalseAndEventDateLessThan(today),
                eventRepository.countByCancelledTrue(),
                registrationRepository.count(),
                registrationRepository.countByCreatedAtGreaterThanEqual(startOfToday),
                volunteerRepository.count(),
                volunteerRepository.countByCreatedAtGreaterThanEqual(startOfToday),
                imageRepository.count(),
                reviewRepository.count(),
                categoryRepository.count(),
                venueRepository.count());
    }
}
