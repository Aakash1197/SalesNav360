package com.salesnav360.dto;

import com.salesnav360.entity.EmailTemplateRecord;
import java.time.Instant;

public record EmailTemplateResponse(
        Long id,
        String name,
        String subject,
        String body,
        String createdBy,
        Instant updatedAt) {

    public static EmailTemplateResponse from(EmailTemplateRecord template) {
        return new EmailTemplateResponse(
                template.getId(), template.getName(), template.getSubject(), template.getBody(),
                template.getCreatedBy(), template.getUpdatedAt());
    }
}
