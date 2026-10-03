package com.feedstartup.repository;

import com.feedstartup.model.EpmPageVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EpmPageVideoRepository extends JpaRepository<EpmPageVideo, String> {
}
