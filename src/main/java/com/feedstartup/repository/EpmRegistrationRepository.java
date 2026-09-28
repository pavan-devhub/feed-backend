package com.feedstartup.repository;

import com.feedstartup.model.EpmRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EpmRegistrationRepository extends JpaRepository<EpmRegistration, Long> {

    boolean existsByEpmEventIdAndMobileNumber(Long epmEventId, String mobileNumber);

    List<EpmRegistration> findAllByOrderByCreatedAtDesc();

    List<EpmRegistration> findByEpmEventIdOrderByCreatedAtDesc(Long epmEventId);

    List<EpmRegistration> findByEventDateOrderByCreatedAtDesc(LocalDate eventDate);

    List<EpmRegistration> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    /** [epmEventId, count] pairs, one per event with at least one registration. */
    @Query("SELECT r.epmEventId, COUNT(r) FROM EpmRegistration r GROUP BY r.epmEventId")
    List<Object[]> countPerEvent();
}
