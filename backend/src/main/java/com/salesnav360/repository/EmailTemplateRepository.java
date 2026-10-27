package com.salesnav360.repository;

import com.salesnav360.entity.EmailTemplateRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplateRecord, Long> {

    @Query("""
            select t from EmailTemplateRecord t
            where (:query is null
                   or lower(t.name) like lower(concat('%', :query, '%'))
                   or lower(t.subject) like lower(concat('%', :query, '%')))
            """)
    Page<EmailTemplateRecord> search(@Param("query") String query, Pageable pageable);
}
