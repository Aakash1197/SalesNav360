package com.salesnav360.repository;

import com.salesnav360.entity.CommunicationHistoryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationHistoryRepository extends JpaRepository<CommunicationHistoryRecord, Long> {
    Page<CommunicationHistoryRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
