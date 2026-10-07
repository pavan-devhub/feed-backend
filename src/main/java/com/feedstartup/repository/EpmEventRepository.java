package com.feedstartup.repository;

import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.model.EpmEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor backs the EPM lists' filters and paging (see EpmEventFilter).
@Repository
public interface EpmEventRepository extends JpaRepository<EpmEvent, Long>, JpaSpecificationExecutor<EpmEvent> {

    Optional<EpmEvent> findByIdAndCancelledFalse(Long id);

    long countByCancelledFalseAndEventDateLessThan(LocalDate before);

    long countByCancelledFalseAndEventDateGreaterThanEqual(LocalDate from);

    long countByCancelledTrue();

    // EPMs held within a date range (both ends included) - the "new EPM" announcements, which go
    // out 15 days before each EPM (see EpmNotificationSchedule).
    List<EpmEvent> findByCancelledFalseAndEventDateBetween(LocalDate from, LocalDate to);

    long countByCategory(String category);

    long countByCategoryAndEventDateGreaterThanEqual(String category, LocalDate from);

    // "Districts Covered" means districts an EPM has actually been held in, so - like EPMs
    // Conducted above - this only counts events that have already happened, not ones still
    // upcoming.
    @Query("SELECT COUNT(DISTINCT e.district) FROM EpmEvent e WHERE e.cancelled = false AND e.eventDate < :before")
    long countDistinctDistricts(LocalDate before);

    @Query("SELECT DISTINCT e.category FROM EpmEvent e WHERE e.category IS NOT NULL")
    List<String> findDistinctCategories();

    // Every place an EPM has been or will be held, cancelled ones included.
    @Query("SELECT DISTINCT new com.feedstartup.dto.EpmLocationDto(e.state, e.district, e.city, e.venue) FROM EpmEvent e")
    List<EpmLocationDto> findDistinctLocations();

    // Renaming a category rewrites the plain-string category on every event that used it.
    @Modifying
    @Query("UPDATE EpmEvent e SET e.category = :newName WHERE e.category = :oldName")
    int renameCategory(String oldName, String newName);
}
