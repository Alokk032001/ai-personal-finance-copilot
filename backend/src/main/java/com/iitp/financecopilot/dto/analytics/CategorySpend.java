package com.iitp.financecopilot.dto.analytics;

import java.math.BigDecimal;

public record CategorySpend(String category, BigDecimal total, long billCount, BigDecimal share) {
}
