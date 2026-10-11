package com.campusfind.service.ai;

import com.campusfind.config.GeminiProperties;
import com.campusfind.entity.Item;
import com.campusfind.repository.ItemRepository;
import com.campusfind.service.matching.MatchingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final GeminiProperties geminiProperties;
    private final GeminiClient geminiClient;
    private final ItemRepository itemRepository;
    private final ObjectMapper objectMapper;
    @Lazy
    private final MatchingService matchingService;

    /**
     * Builds descriptive representation of item for embedding model.
     * Format: Title: {title}. Category: {category}. Description: {description}. Tags: {tags}.
     */
    public String buildItemText(Item item) {
        StringBuilder sb = new StringBuilder();
        if (item.getTitle() != null && !item.getTitle().isBlank()) {
            sb.append("Title: ").append(item.getTitle().trim()).append(". ");
        }
        if (item.getCategory() != null) {
            sb.append("Category: ").append(item.getCategory().name()).append(". ");
        }
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            sb.append("Description: ").append(item.getDescription().trim()).append(". ");
        }
        if (item.getAiTags() != null && !item.getAiTags().isBlank()) {
            sb.append("Tags: ").append(item.getAiTags().trim()).append(". ");
        }
        if (item.getBlock() != null) {
            sb.append("Location: ").append(item.getBlock().getName());
            if (item.getRoom() != null) {
                sb.append(" ").append(item.getRoom().getName());
            }
            sb.append(". ");
        }
        return sb.toString().trim();
    }

    /**
     * Asynchronously generates and stores embedding for a newly created item,
     * then triggers matching re-run so visual similarity contributes to match score.
     */
    @Async
    @Transactional
    public void generateAndSaveEmbeddingAsync(Long itemId) {
        if (!geminiProperties.isGeminiEnabled()) {
            log.debug("Gemini is disabled, skipping embedding generation for item {}", itemId);
            return;
        }

        try {
            Item item = itemRepository.findById(itemId).orElse(null);
            if (item == null) {
                log.warn("Item {} not found for embedding generation", itemId);
                return;
            }

            if (item.getEmbedding() != null && !item.getEmbedding().isBlank()) {
                log.debug("Item {} already has an embedding", itemId);
                return;
            }

            String text = buildItemText(item);
            if (text.isBlank()) {
                return;
            }

            List<Double> vector = geminiClient.generateEmbedding(text);
            if (vector != null && !vector.isEmpty()) {
                String vectorJson = objectMapper.writeValueAsString(vector);
                item.setEmbedding(vectorJson);
                itemRepository.save(item);
                log.info("Saved embedding for item {} (dimension: {})", itemId, vector.size());

                // Trigger matching re-calculation with newly available embedding
                matchingService.runMatchingAsync(itemId);
            }
        } catch (Exception ex) {
            log.warn("Async embedding generation failed for item {}: {}", itemId, ex.getMessage());
        }
    }

    /**
     * Scheduled healing job: every 15 minutes, find items created in the last 48 hours
     * that do not have an embedding, and try generating them (limit 20 items per run).
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void healMissingEmbeddingsJob() {
        if (!geminiProperties.isGeminiEnabled()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<Item> missingItems = itemRepository.findRecentItemsWithoutEmbedding(cutoff, PageRequest.of(0, 20));

        if (missingItems.isEmpty()) {
            return;
        }

        log.info("Running AI embedding healing job: found {} items without embeddings", missingItems.size());
        for (Item item : missingItems) {
            try {
                String text = buildItemText(item);
                if (!text.isBlank()) {
                    List<Double> vector = geminiClient.generateEmbedding(text);
                    if (vector != null && !vector.isEmpty()) {
                        item.setEmbedding(objectMapper.writeValueAsString(vector));
                        itemRepository.save(item);
                        matchingService.runMatchingAsync(item.getId());
                        log.info("Healed missing embedding for item {}", item.getId());
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed healing embedding for item {}: {}", item.getId(), ex.getMessage());
            }
        }
    }

    /**
     * Parse embedding JSON array into List<Double>
     */
    public List<Double> parseEmbedding(String embeddingJson) {
        if (embeddingJson == null || embeddingJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(embeddingJson, new TypeReference<List<Double>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse embedding JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
