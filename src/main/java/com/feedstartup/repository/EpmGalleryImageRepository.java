package com.feedstartup.repository;

import com.feedstartup.model.EpmGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpmGalleryImageRepository extends JpaRepository<EpmGalleryImage, Long> {

    // Block images only - district photos also carry the "epm-gallery" block but have a districtId.
    List<EpmGalleryImage> findByBlockAndDistrictIdIsNull(String block);

    List<EpmGalleryImage> findByBlockInAndDistrictIdIsNull(Collection<String> blocks);

    long countByBlockAndDistrictIdIsNull(String block);

    Optional<EpmGalleryImage> findByIdAndBlockAndDistrictIdIsNull(Long id, String block);

    boolean existsByBlockAndDistrictIdIsNullAndFileName(String block, String fileName);

    List<EpmGalleryImage> findByDistrictIdOrderByDisplayOrderAscIdAsc(Long districtId);

    List<EpmGalleryImage> findByDistrictIdIn(Collection<Long> districtIds);

    Optional<EpmGalleryImage> findByIdAndDistrictId(Long id, Long districtId);

    boolean existsByDistrictIdAndFileName(Long districtId, String fileName);

    long countByDistrictIdIsNotNull();

    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) FROM EpmGalleryImage i WHERE i.block = :block AND i.districtId IS NULL")
    int maxDisplayOrderInBlock(String block);

    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) FROM EpmGalleryImage i WHERE i.districtId = :districtId")
    int maxDisplayOrderInDistrict(Long districtId);
}
