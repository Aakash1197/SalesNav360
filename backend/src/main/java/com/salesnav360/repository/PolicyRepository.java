package com.salesnav360.repository;

import com.salesnav360.entity.PolicyRecord;
import com.salesnav360.enums.PolicyStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<PolicyRecord, Long> {

    @Query("""
            select p from PolicyRecord p
            where (:query is null
                   or lower(p.policyNumber) like lower(concat('%', :query, '%'))
                   or lower(p.policyType) like lower(concat('%', :query, '%'))
                   or lower(p.provider) like lower(concat('%', :query, '%'))
                   or lower(p.client.fullName) like lower(concat('%', :query, '%')))
              and (:status is null or p.status = :status)
              and (:assignedTo is null or lower(p.assignedTo) = lower(:assignedTo))
            """)
    Page<PolicyRecord> search(
            @Param("query") String query,
            @Param("status") PolicyStatus status,
            @Param("assignedTo") String assignedTo,
            Pageable pageable);

    Optional<PolicyRecord> findByPolicyNumberIgnoreCase(String policyNumber);
}

