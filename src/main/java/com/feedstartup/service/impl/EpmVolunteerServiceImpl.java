package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.dto.EpmVolunteerRequestDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmVolunteer;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmVolunteerRepository;
import com.feedstartup.service.EpmVolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EpmVolunteerServiceImpl implements EpmVolunteerService {

    private final EpmVolunteerRepository epmVolunteerRepository;
    private final EpmEventRepository epmEventRepository;

    @Autowired
    public EpmVolunteerServiceImpl(EpmVolunteerRepository epmVolunteerRepository,
                                    EpmEventRepository epmEventRepository) {
        this.epmVolunteerRepository = epmVolunteerRepository;
        this.epmEventRepository = epmEventRepository;
    }

    @Override
    public EpmVolunteerDto volunteer(EpmVolunteerRequestDto dto) {
        EpmEvent event = epmEventRepository.findById(dto.getEpmEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EPM event not found: " + dto.getEpmEventId()));
        if (event.isCancelled()) {
            throw new IllegalArgumentException("This EPM has been cancelled and is no longer accepting volunteers");
        }
        if (epmVolunteerRepository.existsByEpmEventIdAndMobileNumber(event.getId(), dto.getMobileNumber())) {
            throw new IllegalArgumentException("This mobile number has already volunteered for this EPM");
        }

        EpmVolunteer volunteer = new EpmVolunteer();
        volunteer.setEpmEventId(event.getId());
        volunteer.setEventCity(event.getCity());
        volunteer.setEventState(event.getState());
        volunteer.setEventDate(event.getEventDate());
        volunteer.setFullName(dto.getFullName());
        volunteer.setMobileNumber(dto.getMobileNumber());
        volunteer.setEmail(dto.getEmail());
        volunteer.setState(dto.getState());
        volunteer.setDistrict(dto.getDistrict());
        volunteer.setExperience(dto.getExperience());
        volunteer.setReason(dto.getReason());

        return EpmVolunteerDto.from(epmVolunteerRepository.save(volunteer));
    }

    @Override
    public List<EpmVolunteerDto> list(Long epmEventId, LocalDate eventDate, LocalDate submittedOn, String query) {
        // Narrow with the most selective indexed lookup available, then apply the rest in memory.
        List<EpmVolunteer> rows;
        if (epmEventId != null) {
            rows = epmVolunteerRepository.findByEpmEventIdOrderByCreatedAtDesc(epmEventId);
        } else if (eventDate != null) {
            rows = epmVolunteerRepository.findByEventDateOrderByCreatedAtDesc(eventDate);
        } else if (submittedOn != null) {
            rows = epmVolunteerRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    submittedOn.atStartOfDay(), submittedOn.plusDays(1).atStartOfDay());
        } else {
            rows = epmVolunteerRepository.findAllByOrderByCreatedAtDesc();
        }

        Predicate<EpmVolunteer> matches = r -> true;
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
        return rows.stream().filter(matches).map(EpmVolunteerDto::from).collect(Collectors.toList());
    }
}
