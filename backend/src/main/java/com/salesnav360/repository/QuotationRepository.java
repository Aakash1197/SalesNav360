package com.salesnav360.repository;

import com.salesnav360.entity.QuotationRecord;
import com.salesnav360.enums.QuotationStatus;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationRepository extends JpaRepository<QuotationRecord, Long> {

    @Query("""
            select distinct q from QuotationRecord q
            left join q.lead l
            where (:query is null
                   or lower(q.quotationNumber) like lower(concat('%', :query, '%'))
                   or lower(q.client.fullName) like lower(concat('%', :query, '%')))
              and (:status is null or q.status = :status)
            """)
    Page<QuotationRecord> search(
            @Param("query") String query,
            @Param("status") QuotationStatus status,
            Pageable pageable);

    long countByStatus(QuotationStatus status);

    @Query("select coalesce(sum(q.grandTotal), 0) from QuotationRecord q where q.status = :status")
    BigDecimal sumGrandTotalByStatus(@Param("status") QuotationStatus status);
}
