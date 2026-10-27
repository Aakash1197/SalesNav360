package com.salesnav360.controller;

import com.salesnav360.dto.AnalyticsSummaryResponse;
import com.salesnav360.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/salesnav/analytics", "/api/v1/salesnav/reports"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse summary() {
        return analyticsService.getSummary();
    }
}
