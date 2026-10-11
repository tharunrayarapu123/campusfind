package com.campusfind.controller;

import com.campusfind.dto.request.ClaimRequest;
import com.campusfind.dto.request.OfficeClaimRequest;
import com.campusfind.entity.*;
import com.campusfind.repository.*;
import com.campusfind.security.CustomUserDetails;
import com.campusfind.security.JwtService;
import com.campusfind.util.BadgeUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class ClaimAndReturnIntegrationTests {

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
    private ClaimRepository claimRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User studentA; // Lost item owner
    private User studentB; // Found item owner (Finder)
    private User studentC; // Third-party student
    private User adminUser; // Admin

    private String tokenA;
    private String tokenB;
    private String tokenC;
    private String tokenAdmin;

    private Block blockA;
    private Room room101;

    @BeforeEach
    void setUp() {
        blockA = blockRepository.save(Block.builder().name("Block Alpha Test").build());
        room101 = roomRepository.save(Room.builder().block(blockA).name("Room 101").build());

        studentA = userRepository.save(User.builder()
                .fullName("Student Alpha")
                .email("studenta@college.edu")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .department("CS")
                .year(2)
                .karmaPoints(0)
                .role(Role.STUDENT)
                .build());

        studentB = userRepository.save(User.builder()
                .fullName("Student Beta Finder")
                .email("studentb@college.edu")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .department("IT")
                .year(3)
                .karmaPoints(0)
                .role(Role.STUDENT)
                .build());

        studentC = userRepository.save(User.builder()
                .fullName("Student Gamma Stranger")
                .email("studentc@college.edu")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .department("ECE")
                .year(1)
                .karmaPoints(0)
                .role(Role.STUDENT)
                .build());

        adminUser = userRepository.save(User.builder()
                .fullName("Campus Admin")
                .email("claimadmin@college.edu")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .department("Administration")
                .karmaPoints(0)
                .role(Role.ADMIN)
                .build());

        tokenA = jwtService.generateToken(new CustomUserDetails(studentA), studentA.getId(), studentA.getRole().name());
        tokenB = jwtService.generateToken(new CustomUserDetails(studentB), studentB.getId(), studentB.getRole().name());
        tokenC = jwtService.generateToken(new CustomUserDetails(studentC), studentC.getId(), studentC.getRole().name());
        tokenAdmin = jwtService.generateToken(new CustomUserDetails(adminUser), adminUser.getId(), adminUser.getRole().name());
    }

    @Test
    @DisplayName("Claim Verification Flow: RBAC, 429 on 4th attempt, approve confirms match and rejects other matches")
    void testClaimVerificationFlow() throws Exception {
        // 1. Create Lost item by Student A
        Item lostItem = itemRepository.save(Item.builder()
                .owner(studentA)
                .type(ItemType.LOST)
                .title("Black Titan Watch")
                .description("Lost near front desk")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(2))
                .status(ItemStatus.OPEN)
                .build());

        // 2. Create Found item by Student B with verificationQuestion
        Item foundItem = itemRepository.save(Item.builder()
                .owner(studentB)
                .type(ItemType.FOUND)
                .title("Titan Chrono Watch")
                .description("Found on second row desk")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(1))
                .verificationQuestion("What color is the dial strap stitch?")
                .status(ItemStatus.OPEN)
                .build());

        // 3. Create another dummy found item and match to verify rejection of other matches
        Item otherFound = itemRepository.save(Item.builder()
                .owner(studentC)
                .type(ItemType.FOUND)
                .title("Titan Watch duplicate candidate")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(1))
                .verificationQuestion("Any scratches?")
                .status(ItemStatus.OPEN)
                .build());

        MatchEntity primaryMatch = matchRepository.save(MatchEntity.builder()
                .lostItem(lostItem)
                .foundItem(foundItem)
                .score(85)
                .status(MatchStatus.SUGGESTED)
                .build());

        MatchEntity competingMatch = matchRepository.save(MatchEntity.builder()
                .lostItem(lostItem)
                .foundItem(otherFound)
                .score(70)
                .status(MatchStatus.SUGGESTED)
                .build());

        // 4. Stranger (Student C) gets 403 accessing verification question
        mockMvc.perform(get("/api/v1/matches/" + primaryMatch.getId() + "/verification")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        // 5. Lost owner (Student A) gets verification question
        mockMvc.perform(get("/api/v1/matches/" + primaryMatch.getId() + "/verification")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationQuestion").value("What color is the dial strap stitch?"))
                .andExpect(jsonPath("$.data.handedToOffice").value(false));

        // 6. Finder (Student B) cannot submit claim answer (only lost owner can) -> 403
        ClaimRequest attempt1 = new ClaimRequest("Blue stitching");
        mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claim")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attempt1)))
                .andExpect(status().isForbidden());

        // 7. Student A submits 3 claim attempts (all succeed)
        mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claim")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClaimRequest("Red stitching"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("Red stitching"));

        mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claim")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClaimRequest("Orange stitching"))))
                .andExpect(status().isOk());

        MvcResult result3 = mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claim")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClaimRequest("Yellow stitching"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result3.getResponse().getContentAsString());
        Long claim3Id = rootNode.path("data").path("id").asLong();

        // 8. 4th attempt returns 429 Too Many Requests
        mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claim")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClaimRequest("White stitching"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Maximum claim attempts (3) exceeded")));

        // 9. Lost owner (Student A) cannot approve their own claim -> 403
        mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claims/" + claim3Id + "/approve")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // 10. Finder (Student B) approves claim -> Match CONFIRMED, items MATCHED, competing match REJECTED
        mockMvc.perform(post("/api/v1/matches/" + primaryMatch.getId() + "/claims/" + claim3Id + "/approve")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value(true));

        MatchEntity updatedPrimary = matchRepository.findById(primaryMatch.getId()).orElseThrow();
        assertThat(updatedPrimary.getStatus()).isEqualTo(MatchStatus.CONFIRMED);

        Item updatedLost = itemRepository.findById(lostItem.getId()).orElseThrow();
        Item updatedFound = itemRepository.findById(foundItem.getId()).orElseThrow();
        assertThat(updatedLost.getStatus()).isEqualTo(ItemStatus.MATCHED);
        assertThat(updatedFound.getStatus()).isEqualTo(ItemStatus.MATCHED);

        MatchEntity updatedCompeting = matchRepository.findById(competingMatch.getId()).orElseThrow();
        assertThat(updatedCompeting.getStatus()).isEqualTo(MatchStatus.REJECTED);
    }

    @Test
    @DisplayName("Return Flow: sets RETURNED, awards finder +10 karma once, badges computed correctly")
    void testReturnFlowAndBadges() throws Exception {
        Item lostItem = itemRepository.save(Item.builder()
                .owner(studentA)
                .type(ItemType.LOST)
                .title("College ID Card")
                .category(ItemCategory.ID_CARD)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(1))
                .status(ItemStatus.MATCHED)
                .build());

        Item foundItem = itemRepository.save(Item.builder()
                .owner(studentB)
                .type(ItemType.FOUND)
                .title("Found Student ID")
                .category(ItemCategory.ID_CARD)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(1))
                .status(ItemStatus.MATCHED)
                .build());

        MatchEntity match = matchRepository.save(MatchEntity.builder()
                .lostItem(lostItem)
                .foundItem(foundItem)
                .score(90)
                .status(MatchStatus.CONFIRMED)
                .build());

        int initialFinderKarma = studentB.getKarmaPoints();
        assertThat(initialFinderKarma).isEqualTo(0);

        // Stranger cannot mark returned -> 403
        mockMvc.perform(post("/api/v1/matches/" + match.getId() + "/returned")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        // Lost owner marks match as returned
        mockMvc.perform(post("/api/v1/matches/" + match.getId() + "/returned")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));

        // Finder got +10 karma
        User refreshedFinder = userRepository.findById(studentB.getId()).orElseThrow();
        assertThat(refreshedFinder.getKarmaPoints()).isEqualTo(10);

        // Try marking returned again -> 400 Bad Request
        mockMvc.perform(post("/api/v1/matches/" + match.getId() + "/returned")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only CONFIRMED matches can be marked as returned")));

        // Finder karma remained 10 (not doubled)
        refreshedFinder = userRepository.findById(studentB.getId()).orElseThrow();
        assertThat(refreshedFinder.getKarmaPoints()).isEqualTo(10);

        // Verify badge computation logic
        List<String> badges1 = BadgeUtil.computeBadges(1);
        assertThat(badges1).containsExactly("FIRST_RETURN");

        List<String> badges3 = BadgeUtil.computeBadges(3);
        assertThat(badges3).containsExactly("FIRST_RETURN", "HELPER");

        List<String> badges10 = BadgeUtil.computeBadges(10);
        assertThat(badges10).containsExactly("FIRST_RETURN", "HELPER", "TOP_FINDER");

        // Profile endpoint returns badges
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badges", hasItem("FIRST_RETURN")))
                .andExpect(jsonPath("$.data.karmaPoints").value(10));
    }

    @Test
    @DisplayName("Office Drop Point: claim office proof and admin approval flow")
    void testOfficeDropPointFlow() throws Exception {
        Item lostItem = itemRepository.save(Item.builder()
                .owner(studentA)
                .type(ItemType.LOST)
                .title("Lost Water Bottle")
                .category(ItemCategory.BOTTLE)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(2))
                .status(ItemStatus.OPEN)
                .build());

        Item foundItem = itemRepository.save(Item.builder()
                .owner(studentB)
                .type(ItemType.FOUND)
                .title("Steel Flask")
                .category(ItemCategory.BOTTLE)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(1))
                .handedToOffice(true)
                .status(ItemStatus.OPEN)
                .build());

        MatchEntity match = matchRepository.save(MatchEntity.builder()
                .lostItem(lostItem)
                .foundItem(foundItem)
                .score(80)
                .status(MatchStatus.SUGGESTED)
                .build());

        // Verification endpoint indicates handedToOffice = true
        mockMvc.perform(get("/api/v1/matches/" + match.getId() + "/verification")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handedToOffice").value(true));

        // Lost owner submits office proof
        OfficeClaimRequest officeRequest = new OfficeClaimRequest("Has a Marvel superhero sticker at base");
        MvcResult claimResult = mockMvc.perform(post("/api/v1/matches/" + match.getId() + "/claim-office")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(officeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.officeClaim").value(true))
                .andReturn();

        Long claimId = objectMapper.readTree(claimResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Admin approves office claim
        mockMvc.perform(post("/api/v1/admin/matches/" + match.getId() + "/claims/" + claimId + "/office-approve")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value(true));

        MatchEntity updatedMatch = matchRepository.findById(match.getId()).orElseThrow();
        assertThat(updatedMatch.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
    }
}
