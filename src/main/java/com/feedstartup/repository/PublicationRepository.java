package com.feedstartup.repository;

import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * How every multi-row catalog lookup is sorted: newest issue first, then each month's editions
     * by the `order` column (Telugu 1, Hindi 2, English 3 - see PublicationLanguage). The database
     * does the sorting - the generated SQL ends in {@code ORDER BY year DESC, month DESC, `order` ASC}.
     */
    Sort CATALOG_ORDER = Sort.by(Sort.Order.desc("year"), Sort.Order.desc("month"), Sort.Order.asc("order"));

    /** One language's issues within a year, in reading order - January first. */
    Sort CALENDAR_ORDER = Sort.by(Sort.Order.asc("month"));

    Optional<Publication> findByYearAndMonthAndLanguage(Integer year, Integer month, PublicationLanguage language);

    List<Publication> findByYearAndMonth(Integer year, Integer month, Sort sort);

    List<Publication> findByYear(Integer year, Sort sort);

    List<Publication> findByYearAndLanguage(Integer year, PublicationLanguage language, Sort sort);

    /** One page of a year's editions from {@code fromMonth} to {@code toMonth}, both included. */
    Page<Publication> findByYearAndMonthBetween(Integer year, Integer fromMonth, Integer toMonth, Pageable pageable);

    List<Publication> findByTitleContainingIgnoreCase(String title, Sort sort);

    /** Brings rows whose `order` doesn't match their language back in line; returns how many changed. */
    @Modifying
    @Transactional
    @Query("UPDATE Publication p SET p.order = :order WHERE p.language = :language AND p.order <> :order")
    int syncOrder(@Param("language") PublicationLanguage language, @Param("order") int order);
}
