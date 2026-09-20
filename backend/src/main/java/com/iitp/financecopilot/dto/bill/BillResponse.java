package com.iitp.financecopilot.dto.bill;

import com.iitp.financecopilot.domain.BillStatus;
import com.iitp.financecopilot.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BillResponse(
        String id,
        BillStatus status,
        String merchantName,
        String invoiceNumber,
        LocalDate billDate,
        BigDecimal totalAmount,
        String currency,
        ExpenseCategory category,
        Double categoryConfidence,
        String paymentMethod,
        List<BillItemPayload> items,
        String fileName,
        List<String> warnings,
        String aiProvider,
        String aiModel,
        Long aiLatencyMs,
        boolean userEdited,
        Instant createdAt,
        Instant confirmedAt
) {
}
