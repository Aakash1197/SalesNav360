package com.salesnav360.dto;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        long totalPolicies,
        long activePolicies,
        long pendingPolicies,
        long expiredPolicies,
        long renewalsNext30Days,
        BigDecimal activePremiumValue) {
}
