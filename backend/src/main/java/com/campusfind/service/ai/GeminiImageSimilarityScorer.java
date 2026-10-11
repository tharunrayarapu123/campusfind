package com.campusfind.service.ai;

import com.campusfind.config.GeminiProperties;
import com.campusfind.entity.Item;
import com.campusfind.service.matching.ImageSimilarityScorer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("geminiImageSimilarityScorer")
@Primary
@RequiredArgsConstructor
public class GeminiImageSimilarityScorer implements ImageSimilarityScorer {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Override
    public double computeSimilarity(String imageUrl1, String imageUrl2) {
        // Fallback if only URLs are passed: without embeddings, default to 0.0
        return 0.0;
    }

    @Override
    public double computeSimilarity(Item item1, Item item2) {
        if (!geminiProperties.isGeminiEnabled()) {
            return 0.0;
        }

        if (item1 == null || item2 == null) {
            return 0.0;
        }

        List<Double> v1 = parseVector(item1.getEmbedding());
        List<Double> v2 = parseVector(item2.getEmbedding());

        if (v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        double cosine = calculateCosineSimilarity(v1, v2);
        // If similarity is below 0.5, treat as no match (0 points) so noise does not inflate the score
        if (cosine < 0.5) {
            return 0.0;
        }

        return cosine;
    }

    @Override
    public Map<String, Object> getBreakdownDetails(Item item1, Item item2) {
        Map<String, Object> details = new LinkedHashMap<>();

        if (!geminiProperties.isGeminiEnabled()) {
            details.put("imageCompared", false);
            details.put("rawSimilarity", 0.0);
            details.put("reason", "Gemini integration disabled");
            return details;
        }

        if (item1 == null || item2 == null) {
            details.put("imageCompared", false);
            details.put("rawSimilarity", 0.0);
            details.put("reason", "missing item");
            return details;
        }

        List<Double> v1 = parseVector(item1.getEmbedding());
        List<Double> v2 = parseVector(item2.getEmbedding());

        if (v1.isEmpty() || v2.isEmpty()) {
            details.put("imageCompared", false);
            details.put("rawSimilarity", 0.0);
            details.put("reason", "no embedding available");
            return details;
        }

        double cosine = calculateCosineSimilarity(v1, v2);
        double roundedSim = Math.round(cosine * 1000.0) / 1000.0;
        details.put("imageCompared", true);
        details.put("rawSimilarity", roundedSim);

        if (cosine < 0.5) {
            details.put("reason", "similarity (" + roundedSim + ") below 0.5 threshold");
        } else {
            details.put("reason", "matched via Gemini embedding (similarity: " + roundedSim + ")");
        }

        return details;
    }

    public double calculateCosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1 == null || vec2 == null || vec1.isEmpty() || vec2.isEmpty() || vec1.size() != vec2.size()) {
            return 0.0;
        }

        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            double v1 = vec1.get(i);
            double v2 = vec2.get(i);
            dot += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 <= 0.0 || norm2 <= 0.0) {
            return 0.0;
        }

        double similarity = dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return Math.max(0.0, Math.min(1.0, similarity));
    }

    private List<Double> parseVector(String embeddingJson) {
        if (embeddingJson == null || embeddingJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(embeddingJson, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            log.debug("Failed to deserialize embedding vector JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
