package com.iitp.financecopilot.controllers;

import com.iitp.financecopilot.dto.analytics.CategorySpend;
import com.iitp.financecopilot.dto.analytics.DashboardSummary;
import com.iitp.financecopilot.dto.analytics.MonthlySpend;
import com.iitp.financecopilot.security.AuthUser;
import com.iitp.financecopilot.services.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.summary(user);
    }

    @GetMapping("/monthly")
    public List<MonthlySpend> monthly(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "6") int months) {
        return analyticsService.monthly(user, months);
    }

    @GetMapping("/category")
    public List<CategorySpend> category(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        return analyticsService.categorySpend(user, from);
    }
}
