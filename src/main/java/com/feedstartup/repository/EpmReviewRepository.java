package com.feedstartup.repository;

import com.feedstartup.model.EpmReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpmReviewRepository extends JpaRepository<EpmReview, Long> {

    List<EpmReview> findByPublishedTrueOrderByDisplayOrderAscCreatedAtDesc();

    long countByPublishedTrue();

    @Query("SELECT COALESCE(MAX(r.displayOrder), -1) FROM EpmReview r")
    int maxDisplayOrder();
}
