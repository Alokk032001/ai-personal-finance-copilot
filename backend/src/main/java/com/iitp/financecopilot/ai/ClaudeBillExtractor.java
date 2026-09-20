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
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "claude")
public class ClaudeBillExtractor implements BillExtractor {

    private final RestClient restClient;
    private final LlmJson llmJson;
    private final JsonMapper mapper;
    private final String apiKey;
    private final String model;

    public ClaudeBillExtractor(
            RestClient.Builder restClientBuilder,
            LlmJson llmJson,
            JsonMapper mapper,
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model:claude-sonnet-4-5}") String model) {
        this.restClient = restClientBuilder.build();
        this.llmJson = llmJson;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? "claude-sonnet-4-5" : model;
    }

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public ExtractionResult extract(byte[] fileBytes, String contentType) {
        long start = System.currentTimeMillis();
        String mime = contentType == null ? "image/jpeg" : contentType;
        Map<String, Object> source = Map.of(
                "type", "base64",
                "media_type", mime,
                "data", Base64.getEncoder().encodeToString(fileBytes)
        );
        Object filePart = mime.equals("application/pdf")
                ? Map.of("type", "document", "source", source)
                : Map.of("type", "image", "source", source);
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 4096,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                filePart,
                                Map.of("type", "text", "text", ExtractionPrompt.text())
                        )
                ))
        );
        String response;
        try {
            response = restClient.post()
                    .uri("https://api.anthropic.com/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
        String text = extractText(response);
        ExtractedBill extracted = llmJson.parse(text);
        return new ExtractionResult(extracted, text, "claude", model, System.currentTimeMillis() - start);
    }

    private String extractText(String response) {
        if (response == null || response.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.");
        }
        try {
            JsonNode root = mapper.readTree(response);
            JsonNode text = root.at("/content/0/text");
            if (text == null || text.isNull() || text.asString() == null || text.asString().isBlank()) {
                return response;
            }
            return text.asString();
        } catch (Exception ex) {
            return response;
        }
    }
}
