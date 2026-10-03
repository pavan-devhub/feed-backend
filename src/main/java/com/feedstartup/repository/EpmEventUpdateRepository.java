package com.feedstartup.repository;

import com.feedstartup.model.EpmEventUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EpmEventUpdateRepository extends JpaRepository<EpmEventUpdate, Long> {

    // Oldest first - EpmEventChanges#netChanges relies on that order.
    List<EpmEventUpdate> findByEpmEventIdInOrderByCreatedAtAscIdAsc(Collection<Long> epmEventIds);

    void deleteByEpmEventId(Long epmEventId);
}
