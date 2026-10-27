package com.salesnav360.dto;

import com.salesnav360.entity.PolicyRecord;
import com.salesnav360.enums.PolicyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PolicyResponse(
        Long id,
        String policyNumber,
        String policyType,
        Long clientId,
        String clientName,
        String provider,
        BigDecimal premiumAmount,
        BigDecimal coverageAmount,
        LocalDate startDate,
        LocalDate expiryDate,
        LocalDate renewalDate,
        PolicyStatus status,
        String assignedTo,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyResponse from(PolicyRecord policy) {
        return new PolicyResponse(policy.getId(), policy.getPolicyNumber(), policy.getPolicyType(),
                policy.getClient().getId(), policy.getClient().getFullName(), policy.getProvider(),
                policy.getPremiumAmount(), policy.getCoverageAmount(), policy.getStartDate(),
                policy.getExpiryDate(), policy.getRenewalDate(), policy.getStatus(),
                policy.getAssignedTo(), policy.getCreatedAt(), policy.getUpdatedAt());
    }
}

