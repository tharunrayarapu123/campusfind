package com.campusfind.service.matching;

public interface ImageSimilarityScorer {
    /**
     * Compute visual similarity between two item photos, returning a score from 0.0 to 1.0.
     * In Step 4, Google Gemini image embeddings will be plugged in here.
     */
    double computeSimilarity(String imageUrl1, String imageUrl2);
}
