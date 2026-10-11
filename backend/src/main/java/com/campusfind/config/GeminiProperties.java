package com.campusfind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String apiKey;
    private String visionModel = "gemini-1.5-flash";
    private String embeddingModel = "text-embedding-004";
    private int timeoutSeconds = 15;
    private Boolean enabled;

    public boolean isGeminiEnabled() {
        if (enabled != null) {
            return enabled;
        }
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
