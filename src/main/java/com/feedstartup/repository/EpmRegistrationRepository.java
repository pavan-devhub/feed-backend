package com.feedstartup.repository;

import com.feedstartup.model.EpmRegistration;
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
public interface EpmRegistrationRepository extends JpaRepository<EpmRegistration, Long>, JpaSpecificationExecutor<EpmRegistration> {

    boolean existsByEpmEventIdAndMobileNumber(Long epmEventId, String mobileNumber);

    List<EpmRegistration> findByEpmEventId(Long epmEventId);

    // One person's own submissions: those sent while logged in, plus older or signed-out ones
    // made with the account's email or mobile number.
    @Query("SELECT r FROM EpmRegistration r WHERE r.userId = :userId"
            + " OR (r.userId IS NULL AND (LOWER(r.email) = LOWER(:email) OR r.mobileNumber = :phone))")
    List<EpmRegistration> findForUser(Long userId, String email, String phone);

    // Keeps the copied event date/place in step with an EPM that has been rescheduled or moved.
    @Modifying
    @Query("UPDATE EpmRegistration r SET r.eventCity = :city, r.eventState = :state, r.eventDate = :date"
            + " WHERE r.epmEventId = :epmEventId")
    int syncEventSnapshot(Long epmEventId, String city, String state, LocalDate date);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    /** [epmEventId, count] pairs for the given events - one per event with at least one registration. */
    @Query("SELECT r.epmEventId, COUNT(r) FROM EpmRegistration r WHERE r.epmEventId IN :epmEventIds GROUP BY r.epmEventId")
    List<Object[]> countPerEvent(Collection<Long> epmEventIds);
}
