package com.salesnav360.service;

import com.salesnav360.dto.ActivityResponse;
import com.salesnav360.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AuditEventService {

    void record(String actor, String action, String entityType, Long entityId, String summary);

    PageResponse<ActivityResponse> search(Pageable pageable);
}
