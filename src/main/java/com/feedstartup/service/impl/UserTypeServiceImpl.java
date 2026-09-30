package com.feedstartup.service.impl;

import com.feedstartup.dto.UserTypeDto;
import com.feedstartup.model.UserType;
import com.feedstartup.repository.UserTypeRepository;
import com.feedstartup.service.UserTypeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserTypeServiceImpl implements UserTypeService {

    private final UserTypeRepository userTypeRepository;

    public UserTypeServiceImpl(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    @Override
    public List<UserTypeDto> listActive() {
        return userTypeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(UserTypeDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public String resolveActive(String requested) {
        String name = requested == null ? "" : requested.trim();
        return userTypeRepository.findByNameIgnoreCase(name)
                .filter(UserType::isActive)
                .map(UserType::getName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user type \"" + name + "\". Choose one of: "
                        + listActive().stream().map(UserTypeDto::name).collect(Collectors.joining(", "))));
    }
}
