package com.campusfind.service.ai;

import com.campusfind.config.GeminiProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
@Service
public class GeminiClient {

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Getter
    private volatile Instant lastSuccessTime;

    public GeminiClient(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiClient");
        this.retry = retryRegistry.retry("geminiClient");
    }

    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1, properties.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * Sends image bytes to Gemini Vision model with prompt and returns generated raw text.
     */
    public String generateContentWithVision(byte[] imageBytes, String mimeType, String prompt) {
        if (!properties.isGeminiEnabled()) {
            throw new IllegalStateException("Gemini AI integration is disabled or API key is not configured");
        }

        Supplier<String> callSupplier = () -> {
            try {
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                Map<String, Object> textPart = Map.of("text", prompt);
                Map<String, Object> inlineData = Map.of(
                        "mimeType", (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg",
                        "data", base64Image
                );
                Map<String, Object> imagePart = Map.of("inlineData", inlineData);

                Map<String, Object> contentMap = Map.of("parts", List.of(textPart, imagePart));
                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(contentMap),
                        "generationConfig", Map.of("responseMimeType", "application/json")
                );

                String endpoint = "/models/" + properties.getVisionModel() + ":generateContent";
                log.info("Dispatching vision request to Gemini model: {}", properties.getVisionModel());

                String responseBody = buildRestClient().post()
                        .uri(endpoint)
                        .header("x-goog-api-key", properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                this.lastSuccessTime = Instant.now();
                return extractTextFromResponse(responseBody);
            } catch (Exception ex) {
                log.warn("Gemini Vision API request failed: {}", ex.getMessage());
                throw (ex instanceof RuntimeException) ? (RuntimeException) ex : new RuntimeException(ex);
            }
        };

        Supplier<String> retriedSupplier = Retry.decorateSupplier(retry, callSupplier);
        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retriedSupplier);
        return decoratedSupplier.get();
    }

    /**
     * Generates a text embedding vector using the configured Gemini embedding model.
     */
    public List<Double> generateEmbedding(String text) {
        if (!properties.isGeminiEnabled()) {
            throw new IllegalStateException("Gemini AI integration is disabled or API key is not configured");
        }

        Supplier<List<Double>> callSupplier = () -> {
            try {
                String normalizedText = (text != null) ? text.trim() : "";
                if (normalizedText.isEmpty()) {
                    return Collections.emptyList();
                }

                Map<String, Object> requestBody = Map.of(
                        "model", "models/" + properties.getEmbeddingModel(),
                        "content", Map.of("parts", List.of(Map.of("text", normalizedText)))
                );

                String endpoint = "/models/" + properties.getEmbeddingModel() + ":embedContent";
                log.debug("Dispatching text embedding request to model: {}", properties.getEmbeddingModel());

                String responseBody = buildRestClient().post()
                        .uri(endpoint)
                        .header("x-goog-api-key", properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                this.lastSuccessTime = Instant.now();
                return extractEmbeddingValues(responseBody);
            } catch (Exception ex) {
                log.warn("Gemini Embedding API request failed: {}", ex.getMessage());
                throw (ex instanceof RuntimeException) ? (RuntimeException) ex : new RuntimeException(ex);
            }
        };

        Supplier<List<Double>> retriedSupplier = Retry.decorateSupplier(retry, callSupplier);
        Supplier<List<Double>> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retriedSupplier);
        return decoratedSupplier.get();
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            throw new RuntimeException("Malformed Gemini Vision response: missing text parts");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Gemini response: " + ex.getMessage(), ex);
        }
    }

    private List<Double> extractEmbeddingValues(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode values = root.path("embedding").path("values");
            if (values.isArray()) {
                return objectMapper.convertValue(values, new TypeReference<List<Double>>() {});
            }
            throw new RuntimeException("Malformed Gemini Embedding response: missing embedding values");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Gemini embedding: " + ex.getMessage(), ex);
        }
    }
}
