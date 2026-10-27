package com.salesnav360.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsSummaryResponse(
        long totalClients,
        long totalLeads,
        long openLeads,
        long wonDeals,
        double leadConversionRate,
        BigDecimal activePipelineValue,
        BigDecimal closedWonRevenue,
        long activeTasks,
        long completedTasks,
        double taskCompletionRate,
        long activePolicies,
        long renewalsNext30Days,
        long acceptedQuotations,
        long totalQuotations,
        double quotationConversionRate,
        BigDecimal acceptedQuotationValue,
        Map<String, Long> leadStages) {
}
