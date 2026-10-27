package com.salesnav360.repository;

import com.salesnav360.entity.CalendarEventRecord;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEventRecord, Long> {

    @Query("""
            select e from CalendarEventRecord e
            where (:fromDate is null or e.eventDate >= :fromDate)
              and (:toDate is null or e.eventDate <= :toDate)
              and (:query is null
                   or lower(e.title) like lower(concat('%', :query, '%'))
                   or lower(coalesce(e.description, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(e.type, '')) like lower(concat('%', :query, '%')))
            """)
    Page<CalendarEventRecord> search(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
            select case when count(e) > 0 then true else false end
            from CalendarEventRecord e
            where lower(e.title) = lower(:title)
              and e.eventDate = :eventDate
              and ((:startTime is null and e.startTime is null) or e.startTime = :startTime)
              and ((:clientId is null and e.client is null) or e.client.id = :clientId)
              and (:excludeId is null or e.id <> :excludeId)
            """)
    boolean existsDuplicate(
            @Param("title") String title,
            @Param("eventDate") LocalDate eventDate,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("clientId") Long clientId,
            @Param("excludeId") Long excludeId);
}
