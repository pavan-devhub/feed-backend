package com.feedstartup.controller;

import com.feedstartup.dto.ErsStatusDto;
import com.feedstartup.dto.ErsSubmissionDto;
import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.ErsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ers")
@CrossOrigin(origins = "*")
public class ErsController {

    private final ErsService ersService;
    private final UserRepository userRepository;

    @Autowired
    public ErsController(ErsService ersService, UserRepository userRepository) {
        this.ersService = ersService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/submit", produces = "application/json")
    public ResponseEntity<ErsStatusDto> submit(@Valid @RequestBody ErsSubmissionDto submissionDto) {
        Long userId = currentUserId();
        ErsStatusDto status = ersService.submitAssessment(userId, submissionDto.getAnswers());
        return ResponseEntity.ok(status);
    }

    @GetMapping(value = "/status", produces = "application/json")
    public ResponseEntity<Map<String, Object>> status() {
        Long userId = currentUserId();
        return ersService.getLatestStatus(userId)
                .<ResponseEntity<Map<String, Object>>>map(dto -> ResponseEntity.ok(Map.of("attempted", true, "status", dto)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("attempted", false)));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        String email = (String) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getId();
    }
}
