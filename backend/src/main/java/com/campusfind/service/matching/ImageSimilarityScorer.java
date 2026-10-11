package com.campusfind.service.matching;

import com.campusfind.entity.Item;

import java.util.Collections;
import java.util.Map;

public interface ImageSimilarityScorer {

    /**
     * Compute visual similarity between two item photos, returning a score from 0.0 to 1.0.
     */
    double computeSimilarity(String imageUrl1, String imageUrl2);

    /**
     * Compute visual similarity between two items using their embeddings or photos.
     */
    default double computeSimilarity(Item item1, Item item2) {
        return computeSimilarity(
                item1 != null ? item1.getImageUrl() : null,
                item2 != null ? item2.getImageUrl() : null
        );
    }

    /**
     * Returns detailed breakdown map for matching report (imageCompared, rawSimilarity, reason).
     */
    default Map<String, Object> getBreakdownDetails(Item item1, Item item2) {
        return Collections.emptyMap();
    }
}
