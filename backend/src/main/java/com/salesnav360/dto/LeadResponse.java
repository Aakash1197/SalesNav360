package com.salesnav360.dto;

import com.salesnav360.entity.LeadRecord;
import com.salesnav360.enums.LeadPriority;
import com.salesnav360.enums.LeadStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeadResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String companyName,
        BigDecimal expectedValue,
        LeadStage stage,
        LeadPriority priority,
        String source,
        LocalDate followUpDate,
        String assignedTo,
        String notes,
        Long convertedClientId,
        Instant createdAt,
        Instant updatedAt) {

    public static LeadResponse from(LeadRecord record) {
        return new LeadResponse(
                record.getId(), record.getFullName(), record.getEmail(), record.getPhone(),
                record.getCompanyName(), record.getExpectedValue(), record.getStage(),
                record.getPriority(), record.getSource(), record.getFollowUpDate(),
                record.getAssignedTo(), record.getNotes(), record.getConvertedClientId(),
                record.getCreatedAt(), record.getUpdatedAt());
    }
}

