package com.feedstartup.repository;

import com.feedstartup.model.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    List<Publication> findByYearOrderByMonthDesc(Integer year);

    Optional<Publication> findByYearAndMonth(Integer year, Integer month);

    List<Publication> findAllByOrderByYearDescMonthDesc();

    List<Publication> findByTitleContainingIgnoreCaseOrderByYearDescMonthDesc(String query);

    @Query("SELECT p.year AS year, COUNT(p) AS count FROM Publication p GROUP BY p.year ORDER BY p.year DESC")
    List<YearCount> countGroupedByYear();

    interface YearCount {
        Integer getYear();
        Long getCount();
    }
}
