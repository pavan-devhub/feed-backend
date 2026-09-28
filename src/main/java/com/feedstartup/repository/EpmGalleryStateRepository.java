package com.feedstartup.repository;

import com.feedstartup.model.EpmGalleryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpmGalleryStateRepository extends JpaRepository<EpmGalleryState, Long> {

    List<EpmGalleryState> findAllByOrderByDisplayOrderAscNameAsc();

    Optional<EpmGalleryState> findBySlug(String slug);

    Optional<EpmGalleryState> findByFolder(String folder);

    boolean existsBySlug(String slug);

    boolean existsByFolder(String folder);

    @Query("SELECT COALESCE(MAX(s.displayOrder), -1) FROM EpmGalleryState s")
    int maxDisplayOrder();
}
