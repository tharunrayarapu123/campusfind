package com.campusfind.service;

import com.campusfind.entity.*;
import com.campusfind.repository.ItemRepository;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.NotificationRepository;
import com.campusfind.service.matching.DefaultImageSimilarityScorer;
import com.campusfind.service.matching.MatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.campusfind.service.EmailService emailService;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                itemRepository,
                matchRepository,
                notificationRepository,
                new DefaultImageSimilarityScorer(),
                new ObjectMapper(),
                emailService
        );
        ReflectionTestUtils.setField(matchingService, "matchingThreshold", 50);
        ReflectionTestUtils.setField(matchingService, "blockWeight", 25);
        ReflectionTestUtils.setField(matchingService, "roomWeight", 25);
        ReflectionTestUtils.setField(matchingService, "categoryWeight", 20);
        ReflectionTestUtils.setField(matchingService, "timeWeight", 10);
        ReflectionTestUtils.setField(matchingService, "textWeight", 10);
        ReflectionTestUtils.setField(matchingService, "imageWeight", 10);
    }

    @Test
    @DisplayName("High score when same block, same room, same category, similar text and close time")
    void testHighScoreWhenSameLocationAndCategory() {
        Block blockA = Block.builder().id(1L).name("Block A").build();
        Room room101 = Room.builder().id(1L).block(blockA).name("Room 101").build();
        LocalDateTime now = LocalDateTime.now();

        Item lost = Item.builder()
                .id(1L)
                .type(ItemType.LOST)
                .title("Fossil Chronograph Watch")
                .description("Silver metal strap with black dial")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(now)
                .build();

        Item found = Item.builder()
                .id(2L)
                .type(ItemType.FOUND)
                .title("Fossil Watch Found")
                .description("Black dial chronograph watch with silver strap")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(now.plusHours(1)) // within 3 hours -> 10 pts
                .build();

        MatchingService.MatchScoreResult result = matchingService.calculateScore(lost, found);

        // block(25) + room(25) + category(20) + time(10) + text(~10) = 90
        assertThat(result.totalScore).isGreaterThanOrEqualTo(85);
        assertThat(result.breakdown.get("block")).isEqualTo(25);
        assertThat(result.breakdown.get("room")).isEqualTo(25);
        assertThat(result.breakdown.get("category")).isEqualTo(20);
        assertThat(result.breakdown.get("time")).isEqualTo(10);
    }

    @Test
    @DisplayName("Low score when completely different location, category, and text")
    void testLowScoreWhenDifferentEverything() {
        Block blockA = Block.builder().id(1L).name("Block A").build();
        Block blockB = Block.builder().id(2L).name("Block B").build();
        Room room101 = Room.builder().id(1L).block(blockA).name("Room 101").build();
        Room room201 = Room.builder().id(2L).block(blockB).name("Room 201").build();
        LocalDateTime now = LocalDateTime.now();

        Item lost = Item.builder()
                .id(1L)
                .type(ItemType.LOST)
                .title("Casio Watch")
                .description("Gold digital watch")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(now.minusDays(5))
                .build();

        Item found = Item.builder()
                .id(2L)
                .type(ItemType.FOUND)
                .title("Milton Water Bottle")
                .description("Blue thermos flask")
                .category(ItemCategory.BOTTLE)
                .block(blockB)
                .room(room201)
                .eventTime(now)
                .build();

        MatchingService.MatchScoreResult result = matchingService.calculateScore(lost, found);

        // Different everything -> score 0
        assertThat(result.totalScore).isEqualTo(0);
        assertThat(result.breakdown.get("block")).isEqualTo(0);
        assertThat(result.breakdown.get("room")).isEqualTo(0);
        assertThat(result.breakdown.get("category")).isEqualTo(0);
    }

    @Test
    @DisplayName("Time scoring windows: <= 3h gives 10, <= 24h gives 5, > 24h gives 0")
    void testTimeScoringWindows() {
        LocalDateTime baseTime = LocalDateTime.now();

        Item item1 = Item.builder().eventTime(baseTime).build();

        // 1. Within 3 hours -> 10 points
        Item within3h = Item.builder().eventTime(baseTime.plusHours(2)).build();
        MatchingService.MatchScoreResult res1 = matchingService.calculateScore(item1, within3h);
        assertThat(res1.breakdown.get("time")).isEqualTo(10);

        // 2. Within 24 hours -> 5 points
        Item within24h = Item.builder().eventTime(baseTime.plusHours(12)).build();
        MatchingService.MatchScoreResult res2 = matchingService.calculateScore(item1, within24h);
        assertThat(res2.breakdown.get("time")).isEqualTo(5);

        // 3. Beyond 24 hours -> 0 points
        Item beyond24h = Item.builder().eventTime(baseTime.plusHours(36)).build();
        MatchingService.MatchScoreResult res3 = matchingService.calculateScore(item1, beyond24h);
        assertThat(res3.breakdown.get("time")).isEqualTo(0);
    }
}
