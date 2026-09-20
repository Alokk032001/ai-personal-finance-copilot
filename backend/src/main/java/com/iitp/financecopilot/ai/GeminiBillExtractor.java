package com.iitp.financecopilot.ai;

import com.iitp.financecopilot.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiBillExtractor implements BillExtractor {

    private final RestClient restClient;
    private final LlmJson llmJson;
    private final JsonMapper mapper;
    private final String apiKey;
    private final String model;

    public GeminiBillExtractor(
            RestClient.Builder restClientBuilder,
            LlmJson llmJson,
            JsonMapper mapper,
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model:gemini-2.0-flash}") String model) {
        this.restClient = restClientBuilder.build();
        this.llmJson = llmJson;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? "gemini-2.0-flash" : model;
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public ExtractionResult extract(byte[] fileBytes, String contentType) {
        long start = System.currentTimeMillis();
        String mime = contentType == null ? "image/jpeg" : contentType;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", ExtractionPrompt.text()),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mime,
                                        "data", Base64.getEncoder().encodeToString(fileBytes)
                                ))
                        )
                ))
        );
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
        String response;
        try {
            response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
        String text = extractText(response);
        ExtractedBill extracted = llmJson.parse(text);
        return new ExtractionResult(extracted, text, "gemini", model, System.currentTimeMillis() - start);
    }

    private String extractText(String response) {
        if (response == null || response.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
        try {
            JsonNode root = mapper.readTree(response);
            JsonNode text = root.at("/candidates/0/content/parts/0/text");
            if (text == null || text.isNull() || text.asString() == null || text.asString().isBlank()) {
                return response;
            }
            return text.asString();
        } catch (Exception ex) {
            return response;
        }
    }
}
