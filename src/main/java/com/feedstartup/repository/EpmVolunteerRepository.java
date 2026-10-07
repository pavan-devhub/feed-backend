package com.feedstartup.repository;

import com.feedstartup.model.EpmVolunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

// JpaSpecificationExecutor backs the admin list's paging and filters (see EpmSubmissionFilter).
@Repository
public interface EpmVolunteerRepository extends JpaRepository<EpmVolunteer, Long>, JpaSpecificationExecutor<EpmVolunteer> {

    boolean existsByEpmEventIdAndMobileNumber(Long epmEventId, String mobileNumber);

    List<EpmVolunteer> findByEpmEventId(Long epmEventId);

    // One person's own submissions: those sent while logged in, plus older or signed-out ones
    // made with the account's email or mobile number.
    @Query("SELECT r FROM EpmVolunteer r WHERE r.userId = :userId"
            + " OR (r.userId IS NULL AND (LOWER(r.email) = LOWER(:email) OR r.mobileNumber = :phone))")
    List<EpmVolunteer> findForUser(Long userId, String email, String phone);

    // Keeps the copied event date/place in step with an EPM that has been rescheduled or moved.
    @Modifying
    @Query("UPDATE EpmVolunteer r SET r.eventCity = :city, r.eventState = :state, r.eventDate = :date"
            + " WHERE r.epmEventId = :epmEventId")
    int syncEventSnapshot(Long epmEventId, String city, String state, LocalDate date);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    /** [epmEventId, count] pairs for the given events - one per event with at least one volunteer. */
    @Query("SELECT v.epmEventId, COUNT(v) FROM EpmVolunteer v WHERE v.epmEventId IN :epmEventIds GROUP BY v.epmEventId")
    List<Object[]> countPerEvent(Collection<Long> epmEventIds);
}
