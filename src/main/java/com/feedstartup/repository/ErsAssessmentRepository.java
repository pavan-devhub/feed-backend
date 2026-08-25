package com.feedstartup.repository;

import com.feedstartup.model.ErsAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErsAssessmentRepository extends JpaRepository<ErsAssessment, Long> {
    Optional<ErsAssessment> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
