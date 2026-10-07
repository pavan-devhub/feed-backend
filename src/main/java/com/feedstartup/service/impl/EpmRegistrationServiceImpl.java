package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmRegistrationRequestDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmRegistration;
import com.feedstartup.model.UserType;
import com.feedstartup.realtime.LiveUpdateEvents;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.service.EpmRegistrationService;
import com.feedstartup.service.EpmSignUpOwner;
import com.feedstartup.service.EpmSubmissionFilter;
import com.feedstartup.service.UserTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EpmRegistrationServiceImpl implements EpmRegistrationService {

    private final EpmRegistrationRepository epmRegistrationRepository;
    private final EpmEventRepository epmEventRepository;
    private final UserTypeService userTypeService;
    private final EpmSignUpOwner signUpOwner;
    private final ApplicationEventPublisher events;

    @Autowired
    public EpmRegistrationServiceImpl(EpmRegistrationRepository epmRegistrationRepository, EpmEventRepository epmEventRepository,
                                    UserTypeService userTypeService, EpmSignUpOwner signUpOwner,
                                    ApplicationEventPublisher events) {
        this.epmRegistrationRepository = epmRegistrationRepository;
        this.epmEventRepository = epmEventRepository;
        this.userTypeService = userTypeService;
        this.signUpOwner = signUpOwner;
        this.events = events;
    }

    @Override
    public EpmRegistrationDto register(EpmRegistrationRequestDto dto, Long userId) {
        EpmEvent event = epmEventRepository.findById(dto.getEpmEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EPM event not found: " + dto.getEpmEventId()));
        if (event.isCancelled()) {
            throw new IllegalArgumentException("This EPM has been cancelled and is no longer accepting registrations");
        }
        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("This EPM has already taken place and is no longer accepting registrations");
        }
        if (epmRegistrationRepository.existsByEpmEventIdAndMobileNumber(event.getId(), dto.getMobileNumber())) {
            throw new IllegalArgumentException("This mobile number has already registered for this EPM");
        }

        UserType participantType = userTypeService.resolveEpmParticipantType(dto.getParticipantType());

        EpmRegistration registration = new EpmRegistration();
        registration.setEpmEventId(event.getId());
        registration.setUserId(signUpOwner.resolve(userId, dto.getEmail(), dto.getMobileNumber()));
        registration.setEventCity(event.getCity());
        registration.setEventState(event.getState());
        registration.setEventDate(event.getEventDate());
        registration.setFullName(dto.getFullName());
        registration.setMobileNumber(dto.getMobileNumber());
        registration.setEmail(dto.getEmail());
        registration.setState(dto.getState());
        registration.setDistrict(dto.getDistrict());
        registration.setParticipantType(participantType);
        registration.setConsent(dto.isConsent());

        EpmRegistration saved = epmRegistrationRepository.save(registration);
        // Live: the EPM lists' sign-up counts, and this person's own bell / Status of Activities.
        events.publishEvent(new LiveUpdateEvents.EpmSignedUp(saved.getEpmEventId(), saved.getUserId()));
        return EpmRegistrationDto.from(saved);
    }

    @Override
    public PageDto<EpmRegistrationDto> page(EpmSubmissionFilter filter, int page, int size) {
        return PageDto.of(epmRegistrationRepository.findAll(filter.<EpmRegistration>toSpecification(), EpmSubmissionFilter.pageRequest(page, size)),
                EpmRegistrationDto::from);
    }

    @Override
    public List<EpmRegistrationDto> list(EpmSubmissionFilter filter) {
        return epmRegistrationRepository.findAll(filter.<EpmRegistration>toSpecification(), EpmSubmissionFilter.NEWEST_FIRST).stream()
                .map(EpmRegistrationDto::from)
                .collect(Collectors.toList());
    }
}
