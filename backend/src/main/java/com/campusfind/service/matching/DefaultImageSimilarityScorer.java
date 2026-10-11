package com.campusfind.service.matching;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(name = "geminiImageSimilarityScorer")
public class DefaultImageSimilarityScorer implements ImageSimilarityScorer {

    @Override
    public double computeSimilarity(String imageUrl1, String imageUrl2) {
        // Default implementation returns 0.0 until Gemini embeddings are integrated in Step 4
        return 0.0;
    }
}
