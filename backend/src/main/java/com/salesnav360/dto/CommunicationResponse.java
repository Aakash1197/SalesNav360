package com.salesnav360.dto;

import com.salesnav360.entity.CommunicationHistoryRecord;
import java.time.Instant;

public record CommunicationResponse(
        Long id,
        String recipientEmail,
        String subject,
        String status,
        Long clientId,
        String clientName,
        Long leadId,
        String leadName,
        String createdBy,
        Instant createdAt) {

    public static CommunicationResponse from(CommunicationHistoryRecord communication) {
        return new CommunicationResponse(
                communication.getId(),
                communication.getRecipientEmail(),
                communication.getSubject(),
                communication.getStatus(),
                communication.getClient() == null ? null : communication.getClient().getId(),
                communication.getClient() == null ? null : communication.getClient().getFullName(),
                communication.getLead() == null ? null : communication.getLead().getId(),
                communication.getLead() == null ? null : communication.getLead().getFullName(),
                communication.getCreatedBy(),
                communication.getCreatedAt());
    }
}
