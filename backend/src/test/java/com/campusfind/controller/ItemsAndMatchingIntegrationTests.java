package com.campusfind.controller;

import com.campusfind.dto.request.CreateItemRequest;
import com.campusfind.dto.request.UpdateItemRequest;
import com.campusfind.entity.*;
import com.campusfind.repository.*;
import com.campusfind.security.JwtService;
import com.campusfind.service.matching.MatchingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("standalone")
@Transactional
class ItemsAndMatchingIntegrationTests {

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
    private MatchRepository matchRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private User user3;
    private String token1;
    private String token2;
    private String token3;
    private Block blockA;
    private Room room101;

    @BeforeEach
    void setUp() {
        blockA = blockRepository.findByName("Block A").orElseThrow();
        room101 = roomRepository.findByBlockId(blockA.getId()).get(0);

        user1 = userRepository.save(User.builder()
                .fullName("Alice Owner")
                .email("alice.test@college.edu")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .build());

        user2 = userRepository.save(User.builder()
                .fullName("Bob Finder")
                .email("bob.test@college.edu")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .build());

        user3 = userRepository.save(User.builder()
                .fullName("Charlie Other")
                .email("charlie.test@college.edu")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .build());

        token1 = generateToken(user1);
        token2 = generateToken(user2);
        token3 = generateToken(user3);
    }

    private String generateToken(User user) {
        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return jwtService.generateToken(userDetails, user.getId(), user.getRole().name());
    }

    @Test
    @DisplayName("Create LOST and FOUND items: matching creates match and notifications, only owners can view")
    void testCreateLostAndFoundItemsProducesMatchAndNotifications() throws Exception {
        LocalDateTime now = LocalDateTime.now().minusHours(1);

        // 1. User 1 posts LOST watch
        CreateItemRequest lostReq = CreateItemRequest.builder()
                .type(ItemType.LOST)
                .title("Black Fossil Watch")
                .description("Chronograph watch with black strap")
                .category(ItemCategory.WATCH)
                .blockId(blockA.getId())
                .roomId(room101.getId())
                .eventTime(now)
                .build();

        MvcResult lostRes = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lostReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode lostJson = objectMapper.readTree(lostRes.getResponse().getContentAsString());
        Long lostItemId = lostJson.get("data").get("id").asLong();

        // 2. User 2 posts FOUND watch in same block & room
        CreateItemRequest foundReq = CreateItemRequest.builder()
                .type(ItemType.FOUND)
                .title("Found Fossil Watch")
                .description("Black chronograph watch found on desk")
                .category(ItemCategory.WATCH)
                .blockId(blockA.getId())
                .roomId(room101.getId())
                .eventTime(now.plusMinutes(30))
                .verificationQuestion("What is the serial number on the back?")
                .build();

        MvcResult foundRes = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foundReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode foundJson = objectMapper.readTree(foundRes.getResponse().getContentAsString());
        Long foundItemId = foundJson.get("data").get("id").asLong();

        // Trigger synchronous matching for test consistency
        List<MatchEntity> matches = matchingService.runMatchingForItem(foundItemId);
        assertThat(matches).isNotEmpty();

        MatchEntity match = matches.get(0);
        assertThat(match.getScore()).isGreaterThanOrEqualTo(80);
        assertThat(match.getLostItem().getId()).isEqualTo(lostItemId);
        assertThat(match.getFoundItem().getId()).isEqualTo(foundItemId);

        // 3. Verify notifications created for both owners
        List<Notification> notif1 = notificationRepository.findByUserIdOrderByCreatedAtDesc(user1.getId());
        List<Notification> notif2 = notificationRepository.findByUserIdOrderByCreatedAtDesc(user2.getId());
        assertThat(notif1).isNotEmpty();
        assertThat(notif2).isNotEmpty();
        assertThat(notif1.get(0).getBody()).contains("Possible match found");
        assertThat(notif2.get(0).getBody()).contains("Possible match found");

        // 4. Verify Owner 1 can see match
        mockMvc.perform(get("/api/v1/matches/" + match.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score", is(match.getScore())))
                .andExpect(jsonPath("$.data.otherUser.fullName", is("Bob Finder")))
                .andExpect(jsonPath("$.data.otherUser.email").doesNotExist()); // Privacy check!

        // 5. Verify Owner 2 can see match
        mockMvc.perform(get("/api/v1/matches/" + match.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.otherUser.fullName", is("Alice Owner")))
                .andExpect(jsonPath("$.data.otherUser.phone").doesNotExist()); // Privacy check!

        // 6. Third user gets 403 Forbidden
        mockMvc.perform(get("/api/v1/matches/" + match.getId())
                        .header("Authorization", "Bearer " + token3))
                .andExpect(status().isForbidden());

        // 7. Verify duplicate match is not created on second run
        List<MatchEntity> secondRun = matchingService.runMatchingForItem(foundItemId);
        assertThat(secondRun).isEmpty();
    }

    @Test
    @DisplayName("Daily Limit: 11th post in 24 hours returns HTTP 429 Too Many Requests")
    void testDailyPostLimitMax10PerDayReturns429() throws Exception {
        LocalDateTime now = LocalDateTime.now().minusHours(2);

        // Post 10 items
        for (int i = 1; i <= 10; i++) {
            CreateItemRequest req = CreateItemRequest.builder()
                    .type(ItemType.LOST)
                    .title("Lost Item Number " + i)
                    .category(ItemCategory.OTHER)
                    .eventTime(now)
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + token1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 11th post must return 429
        CreateItemRequest eleventhReq = CreateItemRequest.builder()
                .type(ItemType.LOST)
                .title("Eleventh Item Limit Test")
                .category(ItemCategory.OTHER)
                .eventTime(now)
                .build();

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eleventhReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Daily post limit reached")));
    }

    @Test
    @DisplayName("Security: Non-owner cannot edit or delete an item (returns 403)")
    void testNonOwnerCannotEditOrDelete() throws Exception {
        // User 1 creates item
        Item item = itemRepository.save(Item.builder()
                .owner(user1)
                .type(ItemType.LOST)
                .title("Alice Original Item")
                .category(ItemCategory.BOOKS)
                .eventTime(LocalDateTime.now().minusHours(1))
                .build());

        // User 2 tries to edit User 1's item -> 403 Forbidden
        UpdateItemRequest updateReq = UpdateItemRequest.builder()
                .title("Hacked Title by Bob")
                .build();

        mockMvc.perform(put("/api/v1/items/" + item.getId())
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // User 2 tries to delete User 1's item -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/items/" + item.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Upload: Non-image or fake extension rejected by magic byte detection")
    void testInvalidFileTypeRejectedOnUpload() throws Exception {
        // Plain text file pretending to be image
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", "This is plain text, not real image bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/uploads/image")
                        .file(fakeImage)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid image format")));

        // Real JPEG magic bytes (0xFF, 0xD8, 0xFF, ...)
        byte[] realJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 74, 70, 73, 70, 0, 1};
        MockMultipartFile validImage = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", realJpegBytes
        );

        mockMvc.perform(multipart("/api/v1/uploads/image")
                        .file(validImage)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.url", notNullValue()));
    }
}
