package com.salesnav360.dto;

import com.salesnav360.entity.AuditEvent;
import java.time.Instant;

public record ActivityResponse(
        Long id,
        String actor,
        String action,
        String entityType,
        Long entityId,
        String summary,
        Instant createdAt) {

    public static ActivityResponse from(AuditEvent event) {
        return new ActivityResponse(event.getId(), event.getActor(), event.getAction(),
                event.getEntityType(), event.getEntityId(), event.getSummary(), event.getCreatedAt());
    }
}

