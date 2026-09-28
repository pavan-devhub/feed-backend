package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmRegistrationRequestDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmRegistration;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmRegistrationRepository;
import com.feedstartup.service.EpmRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EpmRegistrationServiceImpl implements EpmRegistrationService {

    private final EpmRegistrationRepository epmRegistrationRepository;
    private final EpmEventRepository epmEventRepository;

    @Autowired
    public EpmRegistrationServiceImpl(EpmRegistrationRepository epmRegistrationRepository,
                                       EpmEventRepository epmEventRepository) {
        this.epmRegistrationRepository = epmRegistrationRepository;
        this.epmEventRepository = epmEventRepository;
    }

    @Override
    public EpmRegistrationDto register(EpmRegistrationRequestDto dto) {
        EpmEvent event = epmEventRepository.findById(dto.getEpmEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EPM event not found: " + dto.getEpmEventId()));
        if (event.isCancelled()) {
            throw new IllegalArgumentException("This EPM has been cancelled and is no longer accepting registrations");
        }
        if (epmRegistrationRepository.existsByEpmEventIdAndMobileNumber(event.getId(), dto.getMobileNumber())) {
            throw new IllegalArgumentException("This mobile number has already registered for this EPM");
        }

        EpmRegistration registration = new EpmRegistration();
        registration.setEpmEventId(event.getId());
        registration.setEventCity(event.getCity());
        registration.setEventState(event.getState());
        registration.setEventDate(event.getEventDate());
        registration.setFullName(dto.getFullName());
        registration.setMobileNumber(dto.getMobileNumber());
        registration.setEmail(dto.getEmail());
        registration.setState(dto.getState());
        registration.setDistrict(dto.getDistrict());
        registration.setParticipantType(dto.getParticipantType());
        registration.setConsent(dto.isConsent());

        return EpmRegistrationDto.from(epmRegistrationRepository.save(registration));
    }

    @Override
    public List<EpmRegistrationDto> list(Long epmEventId, LocalDate eventDate, LocalDate submittedOn, String query) {
        // Narrow with the most selective indexed lookup available, then apply the rest in memory.
        List<EpmRegistration> rows;
        if (epmEventId != null) {
            rows = epmRegistrationRepository.findByEpmEventIdOrderByCreatedAtDesc(epmEventId);
        } else if (eventDate != null) {
            rows = epmRegistrationRepository.findByEventDateOrderByCreatedAtDesc(eventDate);
        } else if (submittedOn != null) {
            rows = epmRegistrationRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    submittedOn.atStartOfDay(), submittedOn.plusDays(1).atStartOfDay());
        } else {
            rows = epmRegistrationRepository.findAllByOrderByCreatedAtDesc();
        }

        Predicate<EpmRegistration> matches = r -> true;
        if (eventDate != null) {
            matches = matches.and(r -> eventDate.equals(r.getEventDate()));
        }
        if (submittedOn != null) {
            matches = matches.and(r -> r.getCreatedAt() != null && submittedOn.equals(r.getCreatedAt().toLocalDate()));
        }
        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            matches = matches.and(r -> Stream.of(r.getFullName(), r.getMobileNumber(), r.getEmail(), r.getState(),
                            r.getDistrict(), r.getEventCity())
                    .anyMatch(v -> v != null && v.toLowerCase(Locale.ROOT).contains(q)));
        }
        return rows.stream().filter(matches).map(EpmRegistrationDto::from).collect(Collectors.toList());
    }
}
