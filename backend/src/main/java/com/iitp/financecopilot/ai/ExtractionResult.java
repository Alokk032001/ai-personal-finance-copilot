package com.iitp.financecopilot.ai;

public record ExtractionResult(ExtractedBill extracted, String rawResponse, String provider, String model, long latencyMs) {
}
