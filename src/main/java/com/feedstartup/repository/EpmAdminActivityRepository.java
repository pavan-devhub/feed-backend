package com.feedstartup.repository;

import com.feedstartup.model.EpmAdminActivity;
import com.feedstartup.model.EpmAdminActivity.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * The EPM Activity log is append-only, so this is a bare Repository rather than a JpaRepository:
 * it can add entries and read them, and has no way to change or remove one.
 */
@org.springframework.stereotype.Repository
public interface EpmAdminActivityRepository extends Repository<EpmAdminActivity, Long> {

    <S extends EpmAdminActivity> List<S> saveAll(Iterable<S> activities);

    /**
     * Entries matching every filter given - a null filter matches everything. {@code eventDate} is
     * the EPM's date as it stands now, as the EPM Events list shows it - so it finds every entry of
     * an EPM since rescheduled to that date, and none of one moved off it - or, for an EPM since
     * deleted, the date it had when it was deleted.
     */
    @Query("select a from EpmAdminActivity a"
            + " where (:adminId is null or a.adminId = :adminId)"
            + " and (:action is null or a.action = :action)"
            + " and (:eventDate is null"
            + " or a.epmEventId in (select e.id from EpmEvent e where e.eventDate = :eventDate)"
            + " or (not exists (select e.id from EpmEvent e where e.id = a.epmEventId)"
            + " and a.epmEventId in (select d.epmEventId from EpmAdminActivity d"
            + " where d.action = com.feedstartup.model.EpmAdminActivity.Action.DELETED and d.eventDate = :eventDate)))")
    Page<EpmAdminActivity> search(@Param("adminId") Long adminId, @Param("action") Action action,
                                  @Param("eventDate") LocalDate eventDate, Pageable pageable);

    /** The given EPMs' entries of one kind - DELETED, to show a deleted EPM as it last stood. */
    List<EpmAdminActivity> findByEpmEventIdInAndAction(Collection<Long> epmEventIds, Action action);

    /** [adminId, adminUsername] for every admin with an entry - deleted admins included. */
    @Query("select distinct a.adminId, a.adminUsername from EpmAdminActivity a")
    List<Object[]> findAdmins();
}
