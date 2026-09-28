package com.feedstartup.controller;

import com.feedstartup.dto.EpmCategoryAdminDto;
import com.feedstartup.dto.EpmCategoryRequestDto;
import com.feedstartup.service.EpmCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** EPM event categories. Gated behind the ADMIN role in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/epm/categories")
@CrossOrigin(origins = "*")
public class AdminEpmCategoryController {

    private final EpmCategoryService epmCategoryService;

    @Autowired
    public AdminEpmCategoryController(EpmCategoryService epmCategoryService) {
        this.epmCategoryService = epmCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<EpmCategoryAdminDto>> list() {
        return ResponseEntity.ok(epmCategoryService.listAdmin());
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<EpmCategoryAdminDto> create(@Valid @RequestBody EpmCategoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epmCategoryService.create(dto));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<EpmCategoryAdminDto> update(@PathVariable Long id, @Valid @RequestBody EpmCategoryRequestDto dto) {
        return ResponseEntity.ok(epmCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        epmCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
