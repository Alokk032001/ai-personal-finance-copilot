package com.iitp.financecopilot.dto.bill;

import com.iitp.financecopilot.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** All fields optional. Null means "leave AI value". */
public record BillUpdateRequest(
        String merchantName,
        String invoiceNumber,
        LocalDate billDate,
        BigDecimal totalAmount,
        String currency,
        ExpenseCategory category,
        String paymentMethod,
        List<BillItemPayload> items
) {
}
