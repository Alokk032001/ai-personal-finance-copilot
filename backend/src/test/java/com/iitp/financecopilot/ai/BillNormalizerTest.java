package com.iitp.financecopilot.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import com.iitp.financecopilot.domain.Bill;
import com.iitp.financecopilot.domain.ExpenseCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BillNormalizerTest {

    private final BillNormalizer normalizer = new BillNormalizer();

    @Test
    void happyPathDmartNoWarnings() {
        ExtractedBill extracted = baseDmart();
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals("DMart", bill.getMerchantName());
        assertEquals(new BigDecimal("800.00"), bill.getTotalAmount());
        assertEquals(3, bill.getItems().size());
        assertTrue(bill.getExtraction().getWarnings().isEmpty());
    }

    @Test
    void indianGroupedRupees() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setTotalAmount("Rs. 1,20,499.50");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.9);
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(new BigDecimal("120499.50"), bill.getTotalAmount());
    }

    @Test
    void dayFirstDateNotMonthFirst() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setBillDate("09/03/2026");
        extracted.setTotalAmount("10");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.9);
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(LocalDate.of(2026, Month.MARCH, 9), bill.getBillDate());
    }

    @Test
    void confidencePercentScaled() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setTotalAmount("10");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(97.0);
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(0.97, bill.getCategoryConfidence());
    }

    @Test
    void inventedCategoryBecomesOtherWithWarning() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setTotalAmount("10");
        extracted.setCategory("SNACKS_AND_TREATS");
        extracted.setCategoryConfidence(0.9);
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(ExpenseCategory.OTHER, bill.getCategory());
        assertTrue(bill.getExtraction().getWarnings().stream().anyMatch(w -> w.contains("OTHER")));
    }

    @Test
    void itemSumMismatchKeepsPrintedTotal() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setMerchantName("DMart");
        extracted.setTotalAmount("800");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.97);
        extracted.setItems(List.of(item("A", "100"), item("B", "100"), item("C", "100")));
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(new BigDecimal("800.00"), bill.getTotalAmount());
        assertFalse(bill.getExtraction().getWarnings().isEmpty());
    }

    @Test
    void missingTotalUsesItemSum() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setMerchantName("Store");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.9);
        extracted.setItems(List.of(item("A", "200"), item("B", "180"), item("C", "200")));
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(new BigDecimal("580.00"), bill.getTotalAmount());
        assertTrue(bill.getExtraction().getWarnings().stream().anyMatch(w -> w.toLowerCase().contains("total")));
    }

    @Test
    void allNullExtractDoesNotThrow() {
        Bill bill = assertDoesNotThrow(() -> normalizer.normalizeInto(new Bill(), new ExtractedBill()));
        assertEquals(ExpenseCategory.OTHER, bill.getCategory());
        assertEquals("INR", bill.getCurrency());
        assertEquals(new BigDecimal("0.00"), bill.getTotalAmount());
        assertEquals(LocalDate.now(), bill.getBillDate());
        assertFalse(bill.getExtraction().getWarnings().isEmpty());
    }

    @Test
    void itemCategoryInheritsBillCategory() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setMerchantName("DMart");
        extracted.setTotalAmount("500");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.9);
        extracted.setItems(List.of(item("Rice", "500")));
        extracted.getItems().getFirst().setCategory(null);
        Bill bill = normalizer.normalizeInto(new Bill(), extracted);
        assertEquals(ExpenseCategory.GROCERIES, bill.getItems().getFirst().getCategory());
    }

    private static ExtractedBill baseDmart() {
        ExtractedBill extracted = new ExtractedBill();
        extracted.setMerchantName("DMart");
        extracted.setInvoiceNumber("INV-10291");
        extracted.setBillDate(LocalDate.now().toString());
        extracted.setTotalAmount("800");
        extracted.setCurrency("INR");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.97);
        extracted.setPaymentMethod("UPI");
        extracted.setItems(List.of(item("Rice", "500"), item("Milk", "80"), item("Vegetables", "220")));
        return extracted;
    }

    private static ExtractedBill.ExtractedItem item(String name, String amount) {
        ExtractedBill.ExtractedItem item = new ExtractedBill.ExtractedItem();
        item.setName(name);
        item.setQuantity(1);
        item.setAmount(amount);
        item.setCategory("GROCERIES");
        return item;
    }
}
