package com.feedstartup.repository;

import com.feedstartup.model.EpmCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpmCategoryRepository extends JpaRepository<EpmCategory, Long> {

    List<EpmCategory> findAllByOrderByDisplayOrderAscNameAsc();

    Optional<EpmCategory> findByNameIgnoreCase(String name);
}
