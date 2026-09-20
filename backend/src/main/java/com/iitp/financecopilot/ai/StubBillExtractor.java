package com.iitp.financecopilot.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub", matchIfMissing = true)
public class StubBillExtractor implements BillExtractor {

    @Override
    public String providerName() {
        return "stub";
    }

    @Override
    public ExtractionResult extract(byte[] fileBytes, String contentType) {
        long start = System.currentTimeMillis();
        ExtractedBill extracted = new ExtractedBill();
        extracted.setMerchantName("DMart");
        extracted.setInvoiceNumber("INV-10291");
        extracted.setBillDate(LocalDate.now().toString());
        extracted.setTotalAmount("800");
        extracted.setCurrency("INR");
        extracted.setCategory("GROCERIES");
        extracted.setCategoryConfidence(0.97);
        extracted.setPaymentMethod("UPI");

        ExtractedBill.ExtractedItem rice = new ExtractedBill.ExtractedItem();
        rice.setName("Rice");
        rice.setQuantity(1);
        rice.setAmount("500");
        rice.setCategory("GROCERIES");

        ExtractedBill.ExtractedItem milk = new ExtractedBill.ExtractedItem();
        milk.setName("Milk");
        milk.setQuantity(1);
        milk.setAmount("80");
        milk.setCategory("GROCERIES");

        ExtractedBill.ExtractedItem veg = new ExtractedBill.ExtractedItem();
        veg.setName("Vegetables");
        veg.setQuantity(1);
        veg.setAmount("220");
        veg.setCategory("GROCERIES");

        extracted.setItems(List.of(rice, milk, veg));
        String raw = """
                {"merchantName":"DMart","invoiceNumber":"INV-10291","totalAmount":"800","category":"GROCERIES"}
                """;
        return new ExtractionResult(extracted, raw, "stub", "stub-v1", System.currentTimeMillis() - start);
    }
}
