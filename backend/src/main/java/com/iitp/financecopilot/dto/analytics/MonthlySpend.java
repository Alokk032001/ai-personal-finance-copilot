package com.iitp.financecopilot.dto.analytics;

import java.math.BigDecimal;

public record MonthlySpend(String month, BigDecimal total, long billCount) {
}
