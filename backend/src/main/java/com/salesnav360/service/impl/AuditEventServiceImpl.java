package com.salesnav360.service.impl;

import com.salesnav360.dto.ActivityResponse;
import com.salesnav360.dto.PageResponse;
import com.salesnav360.entity.AuditEvent;
import com.salesnav360.repository.AuditEventRepository;
import com.salesnav360.service.AuditEventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventServiceImpl implements AuditEventService {

    private final AuditEventRepository repository;

    public AuditEventServiceImpl(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(String actor, String action, String entityType, Long entityId, String summary) {
        repository.save(new AuditEvent(actor, action, entityType, entityId, summary));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ActivityResponse> search(Pageable pageable) {
        Pageable bounded = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(repository.findAllByOrderByCreatedAtDesc(bounded).map(ActivityResponse::from));
    }
}
