package com.feedstartup.controller;

import com.feedstartup.dto.UserTypeDto;
import com.feedstartup.service.UserTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (no login - the registration form needs it before anyone has an account): the user
 * types someone can register as, straight from the user_types table.
 */
@RestController
@RequestMapping("/api/user-types")
@CrossOrigin(origins = "*")
public class UserTypeController {

    private final UserTypeService userTypeService;

    public UserTypeController(UserTypeService userTypeService) {
        this.userTypeService = userTypeService;
    }

    @GetMapping
    public ResponseEntity<List<UserTypeDto>> list() {
        return ResponseEntity.ok(userTypeService.listActive());
    }
}
