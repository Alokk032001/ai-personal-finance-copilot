package com.iitp.financecopilot.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummary(
        BigDecimal totalSpend,
        BigDecimal thisMonthSpend,
        long confirmedBillCount,
        long pendingReviewCount,
        String topCategory,
        List<CategorySpend> thisMonthByCategory,
        List<MonthlySpend> recentMonths,
        String currency
) {
}
