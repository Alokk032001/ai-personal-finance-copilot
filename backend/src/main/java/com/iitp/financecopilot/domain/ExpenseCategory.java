package com.iitp.financecopilot.domain;

import java.util.Locale;

public enum ExpenseCategory {
    GROCERIES,
    FOOD_DINING,
    TRANSPORT,
    SHOPPING,
    UTILITIES,
    RENT,
    HEALTHCARE,
    ENTERTAINMENT,
    EDUCATION,
    TRAVEL,
    PERSONAL_CARE,
    INSURANCE,
    INVESTMENT,
    FEES_CHARGES,
    OTHER;

    /**
     * Closed set. Unknown AI values become OTHER. Never throws.
     */
    public static ExpenseCategory fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('/', '_')
                .replace('&', '_');
        try {
            return ExpenseCategory.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }
}
