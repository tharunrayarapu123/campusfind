package com.campusfind.controller;

import com.campusfind.entity.*;
import com.campusfind.repository.*;
import com.campusfind.security.JwtService;
import com.campusfind.service.matching.MatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("standalone")
@Transactional
class AiGeminiWireMockIntegrationTests {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String userToken;
    private Block testBlock;
    private Room testRoom;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("gemini.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("gemini.api-key", () -> "test-gemini-secret-key");
        registry.add("gemini.vision-model", () -> "gemini-1.5-flash");
        registry.add("gemini.embedding-model", () -> "text-embedding-004");
        registry.add("gemini.timeout-seconds", () -> 5);
        registry.add("gemini.enabled", () -> "true");
        registry.add("management.endpoint.health.show-details", () -> "always");
        registry.add("management.endpoint.health.show-components", () -> "always");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        testUser = userRepository.save(User.builder()
                .fullName("AI Tester")
                .email("aitester@college.edu")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .banned(false)
                .build());

        UserDetails userDetails = userDetailsService.loadUserByUsername("aitester@college.edu");
        userToken = "Bearer " + jwtService.generateToken(userDetails, testUser.getId(), testUser.getRole().name());

        testBlock = blockRepository.save(Block.builder().name("Science Block").build());
        testRoom = roomRepository.save(Room.builder().block(testBlock).name("Lab 101").build());
    }

    @Test
    @DisplayName("POST /api/v1/ai/analyze-image succeeds with valid Gemini Vision JSON response")
    void testAnalyzeImageSuccess() throws Exception {
        String geminiResponseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\n  \\"category\\": \\"WATCH\\",\\n  \\"title\\": \\"Casio Vintage Gold Watch\\",\\n  \\"color\\": \\"Gold\\",\\n  \\"brand\\": \\"Casio\\",\\n  \\"tags\\": [\\"watch\\", \\"gold\\", \\"casio\\"],\\n  \\"shortDescription\\": \\"Gold digital wrist watch\\",\\n  \\"sensitiveContent\\": {\\n    \\"hasFace\\": false,\\n    \\"hasIdNumber\\": false,\\n    \\"hasOtherPersonalInfo\\": false\\n  }\\n}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/models/gemini-1.5-flash:generateContent"))
                .withHeader("x-goog-api-key", WireMock.equalTo("test-gemini-secret-key"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(geminiResponseJson)));

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "watch.jpg", "image/jpeg", jpegBytes);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/ai/analyze-image")
                        .file(file)
                        .header("Authorization", userToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.category").value("WATCH"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("Casio Vintage Gold Watch"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.brand").value("Casio"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.color").value("Gold"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.sensitiveContent.hasFace").value(false))
                .andReturn();

        // Security check: Verify API key is NOT leaked anywhere in the response body
        assertThat(result.getResponse().getContentAsString()).doesNotContain("test-gemini-secret-key");

        // Verify WireMock received the authorization header with key
        wireMockServer.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/models/gemini-1.5-flash:generateContent"))
                .withHeader("x-goog-api-key", WireMock.equalTo("test-gemini-secret-key")));
    }

    @Test
    @DisplayName("Gemini returns 429 -> client retries and circuit breaker returns 503 fallback")
    void testAnalyzeImage429Fallback() throws Exception {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/models/gemini-1.5-flash:generateContent"))
                .willReturn(WireMock.aResponse().withStatus(429).withBody("Resource exhausted")));

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "watch.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/ai/analyze-image")
                        .file(file)
                        .header("Authorization", userToken))
                .andExpect(MockMvcResultMatchers.status().isServiceUnavailable())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("AI assistant is temporarily unavailable, please fill in the details manually."));

        // Verify retry occurred (at least 2 attempts made)
        wireMockServer.verify(WireMock.moreThanOrExactly(2), WireMock.postRequestedFor(WireMock.urlEqualTo("/models/gemini-1.5-flash:generateContent")));
    }

    @Test
    @DisplayName("Gemini returns 500 server error -> returns friendly 503 fallback")
    void testAnalyzeImage500Fallback() throws Exception {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/models/gemini-1.5-flash:generateContent"))
                .willReturn(WireMock.aResponse().withStatus(500).withBody("Internal Server Error")));

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "watch.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/ai/analyze-image")
                        .file(file)
                        .header("Authorization", userToken))
                .andExpect(MockMvcResultMatchers.status().isServiceUnavailable())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("AI assistant is temporarily unavailable, please fill in the details manually."));
    }

    @Test
    @DisplayName("Matching engine calculates visual score from Gemini embeddings in scoreBreakdown")
    void testMatchingEngineWithEmbeddings() {
        String embeddingJson = "[0.8, 0.6, 0.0]";

        Item lostItem = itemRepository.save(Item.builder()
                .owner(testUser)
                .type(ItemType.LOST)
                .title("Casio Watch")
                .description("Black digital watch")
                .category(ItemCategory.WATCH)
                .block(testBlock)
                .room(testRoom)
                .eventTime(LocalDateTime.now().minusHours(1))
                .imageUrl("https://res.cloudinary.com/demo/lost_watch.jpg")
                .embedding(embeddingJson)
                .status(ItemStatus.OPEN)
                .build());

        Item foundItem = itemRepository.save(Item.builder()
                .owner(testUser)
                .type(ItemType.FOUND)
                .title("Casio Watch Found")
                .description("Found a black digital watch in science lab")
                .category(ItemCategory.WATCH)
                .block(testBlock)
                .room(testRoom)
                .eventTime(LocalDateTime.now().minusHours(1))
                .imageUrl("https://res.cloudinary.com/demo/found_watch.jpg")
                .embedding(embeddingJson)
                .status(ItemStatus.OPEN)
                .build());

        MatchingService.MatchScoreResult result = matchingService.calculateScore(lostItem, foundItem);

        assertThat(result.totalScore).isGreaterThanOrEqualTo(90);
        assertThat(result.breakdown).containsKey("image");
        assertThat((Number) result.breakdown.get("image")).isEqualTo(10);
        assertThat(result.breakdown.get("imageCompared")).isEqualTo(true);
        assertThat((Double) result.breakdown.get("rawSimilarity")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("GET /api/v1/items/{id}/similar returns top 5 items with owner privacy protection")
    void testGetSimilarItemsWithPrivacy() throws Exception {
        User finder = userRepository.save(User.builder()
                .fullName("Finder Jane")
                .email("finder@college.edu")
                .phone("9876543210")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .role(Role.STUDENT)
                .banned(false)
                .build());

        String targetEmbedding = "[0.7, 0.7, 0.0]";
        String highSimEmbedding = "[0.7, 0.7, 0.0]";
        String lowSimEmbedding = "[-0.7, 0.7, 0.0]";

        Item lostItem = itemRepository.save(Item.builder()
                .owner(testUser)
                .type(ItemType.LOST)
                .title("Lost MacBook Pro")
                .description("Silver M2 laptop")
                .category(ItemCategory.ELECTRONICS)
                .block(testBlock)
                .room(testRoom)
                .eventTime(LocalDateTime.now().minusHours(2))
                .embedding(targetEmbedding)
                .status(ItemStatus.OPEN)
                .build());

        Item highSimFound = itemRepository.save(Item.builder()
                .owner(finder)
                .type(ItemType.FOUND)
                .title("Found Apple Laptop")
                .description("Found silver laptop")
                .category(ItemCategory.ELECTRONICS)
                .block(testBlock)
                .room(testRoom)
                .eventTime(LocalDateTime.now().minusHours(2))
                .embedding(highSimEmbedding)
                .status(ItemStatus.OPEN)
                .build());

        Item lowSimFound = itemRepository.save(Item.builder()
                .owner(finder)
                .type(ItemType.FOUND)
                .title("Found Books")
                .description("Chemistry textbooks")
                .category(ItemCategory.BOOKS)
                .block(testBlock)
                .room(testRoom)
                .eventTime(LocalDateTime.now().minusHours(2))
                .embedding(lowSimEmbedding)
                .status(ItemStatus.OPEN)
                .build());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/items/" + lostItem.getId() + "/similar")
                        .header("Authorization", userToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].item.id").value(highSimFound.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].item.owner.fullName").value("Finder Jane"))
                // STRICT PRIVACY VERIFICATION: owner email and phone must NEVER be exposed
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].item.owner.email").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].item.owner.phone").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("finder@college.edu");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("9876543210");
    }

    @Test
    @DisplayName("GET /actuator/health reports Gemini UP and includes vision and embedding model metadata")
    void testActuatorGeminiHealth() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.gemini.status").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.gemini.details.enabled").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.gemini.details.visionModel").value("gemini-1.5-flash"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.gemini.details.embeddingModel").value("text-embedding-004"));
    }
}
