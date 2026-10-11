package com.campusfind.service.ai;

import com.campusfind.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("gemini")
@RequiredArgsConstructor
public class GeminiHealthIndicator implements HealthIndicator {

    private final GeminiProperties properties;
    private final GeminiClient geminiClient;

    @Override
    public Health health() {
        if (!properties.isGeminiEnabled()) {
            return Health.unknown()
                    .withDetail("enabled", false)
                    .withDetail("reason", "Gemini API key is not configured or integration is disabled")
                    .build();
        }

        Health.Builder builder = Health.up()
                .withDetail("enabled", true)
                .withDetail("visionModel", properties.getVisionModel())
                .withDetail("embeddingModel", properties.getEmbeddingModel());

        if (geminiClient.getLastSuccessTime() != null) {
            builder.withDetail("lastSuccessTime", geminiClient.getLastSuccessTime().toString());
        } else {
            builder.withDetail("lastSuccessTime", "none");
        }

        return builder.build();
    }
}
