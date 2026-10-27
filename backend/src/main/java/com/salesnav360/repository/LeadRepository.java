package com.salesnav360.repository;

import com.salesnav360.entity.LeadRecord;
import com.salesnav360.enums.LeadStage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<LeadRecord, Long> {

    @Query("""
            select l from LeadRecord l
            where (:stage is null or l.stage = :stage)
              and (:assignedTo is null or lower(l.assignedTo) = lower(:assignedTo))
              and (:query is null
                   or lower(l.fullName) like lower(concat('%', :query, '%'))
                   or lower(coalesce(l.email, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(l.companyName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(l.phone, '')) like lower(concat('%', :query, '%')))
            """)
    Page<LeadRecord> search(
            @Param("query") String query,
            @Param("stage") LeadStage stage,
            @Param("assignedTo") String assignedTo,
            Pageable pageable);

    long countByStage(LeadStage stage);

    long countByStageIn(Collection<LeadStage> stages);

    @Query("""
            select count(l) from LeadRecord l
            where l.followUpDate = :date and l.stage not in :terminalStages
            """)
    long countDueOn(@Param("date") LocalDate date, @Param("terminalStages") Collection<LeadStage> terminalStages);

    @Query("select coalesce(sum(l.expectedValue), 0) from LeadRecord l where l.stage in :stages")
    BigDecimal sumExpectedValueByStages(@Param("stages") Collection<LeadStage> stages);

    @Query("select coalesce(sum(l.expectedValue), 0) from LeadRecord l where l.stage = :stage")
    BigDecimal sumExpectedValueByStage(@Param("stage") LeadStage stage);

}

