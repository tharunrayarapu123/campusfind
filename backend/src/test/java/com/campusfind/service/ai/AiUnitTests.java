package com.campusfind.service.ai;

import com.campusfind.config.GeminiProperties;
import com.campusfind.dto.response.ImageAnalysisResponse;
import com.campusfind.entity.ItemCategory;
import com.campusfind.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AiUnitTests {

    private AiAnalysisService aiAnalysisService;
    private GeminiImageSimilarityScorer similarityScorer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setEnabled(true);

        GeminiClient geminiClient = mock(GeminiClient.class);
        org.mockito.Mockito.when(geminiClient.generateContentWithVision(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn("""
                {
                  "category": "WATCH",
                  "title": "Watch",
                  "color": "Black",
                  "brand": "Titan",
                  "tags": ["watch"],
                  "shortDescription": "Watch",
                  "sensitiveContent": { "hasFace": false, "hasIdNumber": false, "hasOtherPersonalInfo": false }
                }
                """);

        aiAnalysisService = new AiAnalysisService(properties, geminiClient, objectMapper);
        similarityScorer = new GeminiImageSimilarityScorer(properties, objectMapper);
    }

    @Test
    @DisplayName("Successfully parses valid JSON from Gemini Vision")
    void testParseValidJson() {
        String json = """
                {
                  "category": "WATCH",
                  "title": "Titan Black Chronograph",
                  "color": "Black",
                  "brand": "Titan",
                  "tags": ["watch", "chronograph", "black strap"],
                  "shortDescription": "A black Titan chronograph watch with leather strap",
                  "sensitiveContent": {
                    "hasFace": false,
                    "hasIdNumber": false,
                    "hasOtherPersonalInfo": false
                  }
                }
                """;

        ImageAnalysisResponse response = aiAnalysisService.parseAndSanitizeAnalysis(json);

        assertThat(response.getCategory()).isEqualTo(ItemCategory.WATCH);
        assertThat(response.getTitle()).isEqualTo("Titan Black Chronograph");
        assertThat(response.getColor()).isEqualTo("Black");
        assertThat(response.getBrand()).isEqualTo("Titan");
        assertThat(response.getTags()).containsExactly("watch", "chronograph", "black strap");
        assertThat(response.getSensitiveContent().isHasFace()).isFalse();
        assertThat(response.getWarningMessage()).isNull();
    }

    @Test
    @DisplayName("Strips markdown code fences (```json ... ```) cleanly")
    void testMarkdownFenceStripping() {
        String markdownOutput = """
                ```json
                {
                  "category": "BOTTLE",
                  "title": "Milton Thermosteel Water Bottle",
                  "color": "Silver",
                  "brand": "Milton",
                  "tags": ["bottle", "steel"],
                  "shortDescription": "Silver insulated bottle",
                  "sensitiveContent": {
                    "hasFace": false,
                    "hasIdNumber": false,
                    "hasOtherPersonalInfo": false
                  }
                }
                ```
                """;

        ImageAnalysisResponse response = aiAnalysisService.parseAndSanitizeAnalysis(markdownOutput);

        assertThat(response.getCategory()).isEqualTo(ItemCategory.BOTTLE);
        assertThat(response.getTitle()).isEqualTo("Milton Thermosteel Water Bottle");
    }

    @Test
    @DisplayName("Maps unknown or invalid categories to OTHER as fallback")
    void testUnknownCategoryFallback() {
        String json = """
                {
                  "category": "UNKNOWN_FURNITURE_ITEM",
                  "title": "Wooden Study Chair",
                  "color": "Brown",
                  "brand": "",
                  "tags": ["chair", "wood"],
                  "shortDescription": "A wooden chair found in hall",
                  "sensitiveContent": {
                    "hasFace": false,
                    "hasIdNumber": false,
                    "hasOtherPersonalInfo": false
                  }
                }
                """;

        ImageAnalysisResponse response = aiAnalysisService.parseAndSanitizeAnalysis(json);

        assertThat(response.getCategory()).isEqualTo(ItemCategory.OTHER);
    }

    @Test
    @DisplayName("Detects sensitive content and populates clear warning message")
    void testSensitiveContentWarning() {
        String jsonWithSensitiveData = """
                {
                  "category": "ID_CARD",
                  "title": "College Student ID Card",
                  "color": "White",
                  "brand": "",
                  "tags": ["id", "card"],
                  "shortDescription": "Student card found on floor",
                  "sensitiveContent": {
                    "hasFace": true,
                    "hasIdNumber": true,
                    "hasOtherPersonalInfo": false
                  }
                }
                """;

        ImageAnalysisResponse response = aiAnalysisService.parseAndSanitizeAnalysis(jsonWithSensitiveData);

        assertThat(response.getSensitiveContent().isHasFace()).isTrue();
        assertThat(response.getSensitiveContent().isHasIdNumber()).isTrue();
        assertThat(response.getWarningMessage()).isNotNull();
        assertThat(response.getWarningMessage()).contains("face");
        assertThat(response.getWarningMessage()).contains("ID card number");
    }

    @Test
    @DisplayName("Enforces 20 requests per hour rate limit per user")
    void testRateLimitEnforcement() {
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", fakeJpeg);
        Long userId = 999L;

        // Simulate 20 calls
        for (int i = 0; i < 20; i++) {
            // Test rate limit helper directly
            aiAnalysisService.analyzeItemImage(userId, file);
        }

        // 21st call should trigger 429 Too Many Requests
        assertThatThrownBy(() -> aiAnalysisService.analyzeItemImage(userId, file))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
    }

    @Test
    @DisplayName("Cosine similarity returns 1.0 for identical vectors and 0.0 for orthogonal vectors")
    void testCosineSimilarityMath() {
        List<Double> v1 = List.of(1.0, 0.0, 0.0);
        List<Double> v2 = List.of(1.0, 0.0, 0.0);
        List<Double> vOrthogonal = List.of(0.0, 1.0, 0.0);

        // Identical vectors -> 1.0
        double simIdentical = similarityScorer.calculateCosineSimilarity(v1, v2);
        assertThat(simIdentical).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));

        // Orthogonal vectors -> 0.0
        double simOrthogonal = similarityScorer.calculateCosineSimilarity(v1, vOrthogonal);
        assertThat(simOrthogonal).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
    }
}
