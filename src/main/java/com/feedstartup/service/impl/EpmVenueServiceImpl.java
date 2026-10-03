package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.EpmVenueDto;
import com.feedstartup.dto.EpmVenueRequestDto;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmVenue;
import com.feedstartup.repository.EpmVenueRepository;
import com.feedstartup.service.EpmVenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EpmVenueServiceImpl implements EpmVenueService {

    private final EpmVenueRepository venueRepository;

    @Autowired
    public EpmVenueServiceImpl(EpmVenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Override
    public List<EpmVenueDto> list() {
        return venueRepository.findAllByOrderByStateAscDistrictAscCityAscNameAsc().stream()
                .map(EpmVenueDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public EpmVenueDto create(EpmVenueRequestDto dto) {
        rejectDuplicate(dto, null);
        EpmVenue venue = new EpmVenue();
        apply(venue, dto);
        return EpmVenueDto.from(venueRepository.save(venue));
    }

    @Override
    public EpmVenueDto update(Long id, EpmVenueRequestDto dto) {
        EpmVenue venue = findOrThrow(id);
        rejectDuplicate(dto, id);
        apply(venue, dto);
        return EpmVenueDto.from(venueRepository.save(venue));
    }

    @Override
    public void delete(Long id) {
        venueRepository.delete(findOrThrow(id));
    }

    @Override
    public void recordIfNew(EpmLocationDto location) {
        String name = location.venue().trim();
        String city = location.city().trim();
        String district = location.district().trim();
        String state = location.state().trim();
        if (venueRepository.existsByNameIgnoreCaseAndCityIgnoreCaseAndDistrictIgnoreCaseAndStateIgnoreCase(
                name, city, district, state)) {
            return;
        }
        EpmVenue venue = new EpmVenue();
        venue.setName(name);
        venue.setCity(city);
        venue.setDistrict(district);
        venue.setState(state);
        venueRepository.save(venue);
    }

    private static void apply(EpmVenue venue, EpmVenueRequestDto dto) {
        venue.setName(dto.name().trim());
        venue.setState(dto.state().trim());
        venue.setDistrict(dto.district().trim());
        venue.setCity(dto.city().trim());
        venue.setAddress(dto.address() == null || dto.address().isBlank() ? null : dto.address().trim());
    }

    // Checked here rather than with a unique index: four 255-char columns are too wide for a
    // MySQL utf8mb4 index key.
    private void rejectDuplicate(EpmVenueRequestDto dto, Long selfId) {
        boolean duplicate = venueRepository.findAll().stream()
                .filter(v -> selfId == null || !v.getId().equals(selfId))
                .anyMatch(v -> v.getName().equalsIgnoreCase(dto.name().trim())
                        && v.getCity().equalsIgnoreCase(dto.city().trim())
                        && v.getDistrict().equalsIgnoreCase(dto.district().trim())
                        && v.getState().equalsIgnoreCase(dto.state().trim()));
        if (duplicate) {
            throw new ConflictException("\"" + dto.name().trim() + "\" in " + dto.city().trim() + " is already in the venue list");
        }
    }

    private EpmVenue findOrThrow(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
    }
}
