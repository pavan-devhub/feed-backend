package com.feedstartup.repository;

import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {
    Optional<Publication> findByYearAndMonthAndLanguage(Integer year, Integer month, PublicationLanguage language);

    List<Publication> findByYearAndMonth(Integer year, Integer month);

    List<Publication> findByYear(Integer year);

    List<Publication> findByTitleContainingIgnoreCase(String title);
}
