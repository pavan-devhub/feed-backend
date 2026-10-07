package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.dto.EpmVolunteerRequestDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmVolunteer;
import com.feedstartup.model.UserType;
import com.feedstartup.realtime.LiveUpdateEvents;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmSignUpOwner;
import com.feedstartup.service.EpmVolunteerService;
import com.feedstartup.service.EpmSubmissionFilter;
import com.feedstartup.service.UserTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EpmVolunteerServiceImpl implements EpmVolunteerService {

    private final EpmVolunteerRepository epmVolunteerRepository;
    private final EpmEventRepository epmEventRepository;
    private final UserTypeService userTypeService;
    private final EpmSignUpOwner signUpOwner;
    private final ApplicationEventPublisher events;

    @Autowired
    public EpmVolunteerServiceImpl(EpmVolunteerRepository epmVolunteerRepository, EpmEventRepository epmEventRepository,
                                   UserTypeService userTypeService, EpmSignUpOwner signUpOwner,
                                   ApplicationEventPublisher events) {
        this.epmVolunteerRepository = epmVolunteerRepository;
        this.epmEventRepository = epmEventRepository;
        this.userTypeService = userTypeService;
        this.signUpOwner = signUpOwner;
        this.events = events;
    }

    @Override
    public EpmVolunteerDto volunteer(EpmVolunteerRequestDto dto, Long userId) {
        EpmEvent event = epmEventRepository.findById(dto.getEpmEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EPM event not found: " + dto.getEpmEventId()));
        if (event.isCancelled()) {
            throw new IllegalArgumentException("This EPM has been cancelled and is no longer accepting volunteers");
        }
        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("This EPM has already taken place and is no longer accepting volunteers");
        }
        if (epmVolunteerRepository.existsByEpmEventIdAndMobileNumber(event.getId(), dto.getMobileNumber())) {
            throw new IllegalArgumentException("This mobile number has already volunteered for this EPM");
        }

        UserType participantType = userTypeService.resolveEpmParticipantType(dto.getParticipantType());

        EpmVolunteer volunteer = new EpmVolunteer();
        volunteer.setEpmEventId(event.getId());
        volunteer.setUserId(signUpOwner.resolve(userId, dto.getEmail(), dto.getMobileNumber()));
        volunteer.setEventCity(event.getCity());
        volunteer.setEventState(event.getState());
        volunteer.setEventDate(event.getEventDate());
        volunteer.setFullName(dto.getFullName());
        volunteer.setMobileNumber(dto.getMobileNumber());
        volunteer.setEmail(dto.getEmail());
        volunteer.setState(dto.getState());
        volunteer.setDistrict(dto.getDistrict());
        volunteer.setParticipantType(participantType);
        volunteer.setReason(dto.getReason());

        EpmVolunteer saved = epmVolunteerRepository.save(volunteer);
        // Live: the EPM lists' sign-up counts, and this person's own bell / Status of Activities.
        events.publishEvent(new LiveUpdateEvents.EpmSignedUp(saved.getEpmEventId(), saved.getUserId()));
        return EpmVolunteerDto.from(saved);
    }

    @Override
    public PageDto<EpmVolunteerDto> page(EpmSubmissionFilter filter, int page, int size) {
        return PageDto.of(epmVolunteerRepository.findAll(filter.<EpmVolunteer>toSpecification(), EpmSubmissionFilter.pageRequest(page, size)),
                EpmVolunteerDto::from);
    }

    @Override
    public List<EpmVolunteerDto> list(EpmSubmissionFilter filter) {
        return epmVolunteerRepository.findAll(filter.<EpmVolunteer>toSpecification(), EpmSubmissionFilter.NEWEST_FIRST).stream()
                .map(EpmVolunteerDto::from)
                .collect(Collectors.toList());
    }
}
