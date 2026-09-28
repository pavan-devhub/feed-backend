package com.feedstartup.repository;

import com.feedstartup.model.EpmVolunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EpmVolunteerRepository extends JpaRepository<EpmVolunteer, Long> {

    boolean existsByEpmEventIdAndMobileNumber(Long epmEventId, String mobileNumber);

    List<EpmVolunteer> findAllByOrderByCreatedAtDesc();

    List<EpmVolunteer> findByEpmEventIdOrderByCreatedAtDesc(Long epmEventId);

    List<EpmVolunteer> findByEventDateOrderByCreatedAtDesc(LocalDate eventDate);

    List<EpmVolunteer> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    /** [epmEventId, count] pairs, one per event with at least one volunteer. */
    @Query("SELECT v.epmEventId, COUNT(v) FROM EpmVolunteer v GROUP BY v.epmEventId")
    List<Object[]> countPerEvent();
}
