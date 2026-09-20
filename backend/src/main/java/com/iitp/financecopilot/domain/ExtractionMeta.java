package com.iitp.financecopilot.domain;

import java.util.ArrayList;
import java.util.List;

public class ExtractionMeta {

    private String provider;
    private String model;
    private long latencyMs;
    private List<String> warnings = new ArrayList<>();
    /** Kept in Mongo only — never returned in API DTOs. */
    private String rawResponse;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
