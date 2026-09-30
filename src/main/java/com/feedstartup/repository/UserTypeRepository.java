package com.feedstartup.repository;

import com.feedstartup.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTypeRepository extends JpaRepository<UserType, Long> {

    /** What the registration form offers, in dropdown order. */
    List<UserType> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<UserType> findByNameIgnoreCase(String name);
}
