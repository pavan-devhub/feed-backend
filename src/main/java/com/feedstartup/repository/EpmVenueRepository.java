package com.feedstartup.repository;

import com.feedstartup.model.EpmVenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpmVenueRepository extends JpaRepository<EpmVenue, Long> {

    List<EpmVenue> findAllByOrderByStateAscDistrictAscCityAscNameAsc();
}
