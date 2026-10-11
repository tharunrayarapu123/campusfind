package com.campusfind.service.matching;

import com.campusfind.entity.Item;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("defaultImageSimilarityScorer")
@ConditionalOnMissingBean(name = "geminiImageSimilarityScorer")
public class DefaultImageSimilarityScorer implements ImageSimilarityScorer {

    @Override
    public double computeSimilarity(String imageUrl1, String imageUrl2) {
        return 0.0;
    }

    @Override
    public double computeSimilarity(Item item1, Item item2) {
        return 0.0;
    }

    @Override
    public Map<String, Object> getBreakdownDetails(Item item1, Item item2) {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean compared = item1 != null && item2 != null && item1.getImageUrl() != null && item2.getImageUrl() != null;
        details.put("imageCompared", compared);
        details.put("rawSimilarity", 0.0);
        details.put("reason", compared ? "image scorer not configured" : "missing image");
        return details;
    }
}
