package com.feedstartup.repository;

import com.feedstartup.model.EpmRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpmRegistrationRepository extends JpaRepository<EpmRegistration, Long> {

    boolean existsByEpmEventIdAndMobileNumber(Long epmEventId, String mobileNumber);

    List<EpmRegistration> findAllByOrderByCreatedAtDesc();

    List<EpmRegistration> findByEpmEventIdOrderByCreatedAtDesc(Long epmEventId);
}
