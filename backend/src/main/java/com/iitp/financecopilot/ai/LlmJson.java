package com.iitp.financecopilot.ai;

import com.iitp.financecopilot.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LlmJson {

    private final JsonMapper mapper;

    public LlmJson(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public ExtractedBill parse(String raw) {
        String json = isolateObject(raw);
        try {
            ExtractedBill parsed = mapper.readValue(json, ExtractedBill.class);
            if (parsed == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
            }
            return parsed;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
    }

    static String isolateObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
        return raw.substring(start, end + 1);
    }
}
