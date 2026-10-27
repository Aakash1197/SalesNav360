package com.salesnav360.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalClients,
        long totalInvestors,
        long totalLeads,
        long openLeads,
        long wonDeals,
        long followUpsDueToday,
        double conversionRate,
        BigDecimal closedWonRevenue,
        BigDecimal averageDealSize,
        BigDecimal activePipelineValue,
        long activeTasks,
        long completedTasks,
        long overdueTasks,
        List<TaskResponse> upcomingTasks,
        Map<String, Long> leadStages,
        List<ActivityResponse> recentActivity) {
}

