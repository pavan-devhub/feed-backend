package com.feedstartup.repository;

import com.feedstartup.model.DataMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataMigrationRepository extends JpaRepository<DataMigration, String> {
}
