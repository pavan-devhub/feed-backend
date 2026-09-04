package com.feedstartup.repository;

import com.feedstartup.model.EpmVolunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpmVolunteerRepository extends JpaRepository<EpmVolunteer, Long> {

    boolean existsByEpmEventIdAndMobileNumber(Long epmEventId, String mobileNumber);

    List<EpmVolunteer> findAllByOrderByCreatedAtDesc();

    List<EpmVolunteer> findByEpmEventIdOrderByCreatedAtDesc(Long epmEventId);
}
