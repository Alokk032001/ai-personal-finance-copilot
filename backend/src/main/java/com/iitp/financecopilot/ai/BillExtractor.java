package com.iitp.financecopilot.ai;

public interface BillExtractor {

    String providerName();

    ExtractionResult extract(byte[] fileBytes, String contentType);
}
