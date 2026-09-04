package com.feedstartup.repository;

import com.feedstartup.model.EpmGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpmGalleryImageRepository extends JpaRepository<EpmGalleryImage, Long> {

    List<EpmGalleryImage> findAllByOrderByDisplayOrderAscIdDesc();
}
