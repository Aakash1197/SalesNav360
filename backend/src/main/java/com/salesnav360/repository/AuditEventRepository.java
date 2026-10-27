package com.salesnav360.repository;

import com.salesnav360.entity.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop10ByOrderByCreatedAtDesc();

    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

