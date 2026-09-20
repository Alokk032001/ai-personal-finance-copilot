package com.iitp.financecopilot.ai;

public final class ExtractionPrompt {

    private ExtractionPrompt() {
    }

    public static String text() {
        return """
                Extract structured data from this bill, invoice, or receipt.
                Return JSON only, no markdown fences, no prose.

                Shape:
                {
                  "merchantName": string or null,
                  "invoiceNumber": string or null,
                  "billDate": string or null,
                  "totalAmount": string or null,
                  "currency": string or null,
                  "category": string or null,
                  "categoryConfidence": number or null,
                  "paymentMethod": string or null,
                  "items": [{"name": string, "quantity": number or string, "amount": string or number, "category": string or null}]
                }

                Rules:
                - ISO dates with Indian DD/MM/YYYY interpretation (day-first).
                - Amounts as plain numbers; totalAmount is the grand total after tax/discount.
                - Currency ISO-4217, default INR.
                - category MUST be one of GROCERIES, FOOD_DINING, TRANSPORT, SHOPPING, UTILITIES, RENT, HEALTHCARE, ENTERTAINMENT, EDUCATION, TRAVEL, PERSONAL_CARE, INSURANCE, INVESTMENT, FEES_CHARGES, OTHER. Never invent.
                - categoryConfidence is honest 0-1 (not 0-100).
                - Item categories from the same closed list.
                - paymentMethod examples: UPI, CASH, CARD, NETBANKING, WALLET. Null if not visible. Never invent.
                - If this is not a bill, set all fields null, category OTHER, categoryConfidence 0, items [].
                """;
    }
}
