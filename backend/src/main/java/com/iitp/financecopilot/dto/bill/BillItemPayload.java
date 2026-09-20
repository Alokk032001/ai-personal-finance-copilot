package com.iitp.financecopilot.dto.bill;

import com.iitp.financecopilot.domain.ExpenseCategory;

import java.math.BigDecimal;

public record BillItemPayload(
        String name,
        BigDecimal quantity,
        BigDecimal amount,
        ExpenseCategory category
) {
}
