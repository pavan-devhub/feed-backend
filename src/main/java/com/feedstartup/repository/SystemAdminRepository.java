package com.feedstartup.repository;

import com.feedstartup.model.SystemAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemAdminRepository extends JpaRepository<SystemAdmin, Long> {
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByMobileNumber(String mobileNumber);
    boolean existsByEmailIgnoreCase(String email);
    Optional<SystemAdmin> findByUsernameIgnoreCase(String username);
    Optional<SystemAdmin> findByMobileNumber(String mobileNumber);
    Optional<SystemAdmin> findByEmailIgnoreCase(String email);
    List<SystemAdmin> findAllByOrderByCreatedAtAscIdAsc();
    List<SystemAdmin> findByCreatedBy_Id(Long createdById);
}
