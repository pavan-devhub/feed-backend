package com.feedstartup.repository;

import com.feedstartup.model.EpmGalleryDistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpmGalleryDistrictRepository extends JpaRepository<EpmGalleryDistrict, Long> {

    List<EpmGalleryDistrict> findByStateIdOrderByDisplayOrderAscNameAsc(Long stateId);

    List<EpmGalleryDistrict> findByStateIdIn(Collection<Long> stateIds);

    Optional<EpmGalleryDistrict> findByStateIdAndSlug(Long stateId, String slug);

    Optional<EpmGalleryDistrict> findByStateIdAndFolder(Long stateId, String folder);

    boolean existsByStateIdAndSlug(Long stateId, String slug);

    boolean existsByStateIdAndFolder(Long stateId, String folder);

    @Query("SELECT COALESCE(MAX(d.displayOrder), -1) FROM EpmGalleryDistrict d WHERE d.stateId = :stateId")
    int maxDisplayOrderInState(Long stateId);
}
