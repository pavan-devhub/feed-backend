package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmCategoryAdminDto;
import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmCategoryRequestDto;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmCategory;
import com.feedstartup.repository.EpmCategoryRepository;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.service.EpmCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class EpmCategoryServiceImpl implements EpmCategoryService {

    private final EpmCategoryRepository categoryRepository;
    private final EpmEventRepository eventRepository;

    @Autowired
    public EpmCategoryServiceImpl(EpmCategoryRepository categoryRepository, EpmEventRepository eventRepository) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<EpmCategoryDto> listPublic() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(EpmCategoryDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<EpmCategoryAdminDto> listAdmin() {
        LocalDate today = LocalDate.now();
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> EpmCategoryAdminDto.from(c,
                        eventRepository.countByCategory(c.getName()),
                        eventRepository.countByCategoryAndEventDateGreaterThanEqual(c.getName(), today)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EpmCategoryAdminDto create(EpmCategoryRequestDto dto) {
        String name = dto.name().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("A category called \"" + name + "\" already exists");
        }
        EpmCategory category = new EpmCategory();
        category.setName(name);
        apply(category, dto);
        if (dto.displayOrder() == null) {
            category.setDisplayOrder((int) categoryRepository.count());
        }
        return toAdminDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public EpmCategoryAdminDto update(Long id, EpmCategoryRequestDto dto) {
        EpmCategory category = findOrThrow(id);
        String newName = dto.name().trim();
        categoryRepository.findByNameIgnoreCase(newName)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("A category called \"" + newName + "\" already exists"); });

        String oldName = category.getName();
        category.setName(newName);
        apply(category, dto);
        EpmCategory saved = categoryRepository.save(category);
        if (!oldName.equals(newName)) {
            eventRepository.renameCategory(oldName, newName);
        }
        return toAdminDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EpmCategory category = findOrThrow(id);
        long inUse = eventRepository.countByCategory(category.getName());
        if (inUse > 0) {
            throw new ConflictException("\"" + category.getName() + "\" is used by " + inUse
                    + (inUse == 1 ? " EPM - move it" : " EPMs - move them") + " to another category first");
        }
        categoryRepository.delete(category);
    }

    @Override
    public String resolveName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Category is required - choose one of: " + allowedNames());
        }
        return categoryRepository.findByNameIgnoreCase(raw.trim())
                .map(EpmCategory::getName)
                .orElseThrow(() -> new IllegalArgumentException("Category must be one of: " + allowedNames()));
    }

    private void apply(EpmCategory category, EpmCategoryRequestDto dto) {
        category.setLabel(dto.label() == null || dto.label().isBlank() ? category.getName() : dto.label().trim());
        String color = dto.color() == null || dto.color().isBlank() ? "gray" : dto.color().trim().toLowerCase(Locale.ROOT);
        if (!EpmCategory.COLORS.contains(color)) {
            throw new IllegalArgumentException("Colour must be one of: " + String.join(", ", EpmCategory.COLORS));
        }
        category.setColor(color);
        if (dto.displayOrder() != null) category.setDisplayOrder(dto.displayOrder());
    }

    private String allowedNames() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(EpmCategory::getName)
                .collect(Collectors.joining(", "));
    }

    private EpmCategoryAdminDto toAdminDto(EpmCategory c) {
        return EpmCategoryAdminDto.from(c, eventRepository.countByCategory(c.getName()),
                eventRepository.countByCategoryAndEventDateGreaterThanEqual(c.getName(), LocalDate.now()));
    }

    private EpmCategory findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EPM category not found: " + id));
    }
}
