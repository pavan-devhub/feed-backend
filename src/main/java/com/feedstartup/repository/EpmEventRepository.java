package com.feedstartup.repository;

import com.feedstartup.model.EpmEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpmEventRepository extends JpaRepository<EpmEvent, Long> {

    List<EpmEvent> findByCancelledFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate from);

    List<EpmEvent> findByCancelledFalseAndEventDateLessThanOrderByEventDateDesc(LocalDate before);

    List<EpmEvent> findByCancelledFalseOrderByEventDateDesc();

    Optional<EpmEvent> findByIdAndCancelledFalse(Long id);

    long countByCancelledFalseAndEventDateLessThan(LocalDate before);

    // "Districts Covered" means districts an EPM has actually been held in, so - like EPMs
    // Conducted above - this only counts events that have already happened, not ones still
    // upcoming.
    @Query("SELECT COUNT(DISTINCT e.district) FROM EpmEvent e WHERE e.cancelled = false AND e.eventDate < :before")
    long countDistinctDistricts(LocalDate before);
}
