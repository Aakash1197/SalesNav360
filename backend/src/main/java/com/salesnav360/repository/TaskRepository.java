package com.salesnav360.repository;

import com.salesnav360.entity.TaskRecord;
import com.salesnav360.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskRecord, Long> {

    @Query("""
            select t from TaskRecord t
            where (:query is null or lower(t.title) like lower(concat('%', :query, '%'))
                   or lower(coalesce(t.description, '')) like lower(concat('%', :query, '%')))
              and (:status is null or t.status = :status)
              and (:assignedTo is null or lower(t.assignedTo) = lower(:assignedTo))
            """)
    Page<TaskRecord> search(
            @Param("query") String query,
            @Param("status") TaskStatus status,
            @Param("assignedTo") String assignedTo,
            Pageable pageable);

    long countByStatusIn(Set<TaskStatus> statuses);

    long countByStatus(TaskStatus status);

    long countByDueDateBeforeAndStatusIn(LocalDate date, Set<TaskStatus> statuses);

    List<TaskRecord> findTop5ByStatusInAndDueDateGreaterThanEqualOrderByDueDateAsc(Set<TaskStatus> statuses, LocalDate date);
}

