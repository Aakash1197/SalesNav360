package com.salesnav360.dto;

import com.salesnav360.entity.ClientRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.ClientType;
import java.time.Instant;

public record ClientResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String companyName,
        ClientType type,
        ClientStatus status,
        String leadSource,
        String assignedTo,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static ClientResponse from(ClientRecord record) {
        return new ClientResponse(
                record.getId(), record.getFullName(), record.getEmail(), record.getPhone(),
                record.getCompanyName(), record.getType(), record.getStatus(), record.getLeadSource(),
                record.getAssignedTo(), record.getNotes(), record.getCreatedAt(), record.getUpdatedAt());
    }
}

