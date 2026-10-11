package com.campusfind.service.matching;

import com.campusfind.entity.*;
import com.campusfind.repository.ItemRepository;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final ItemRepository itemRepository;
    private final MatchRepository matchRepository;
    private final NotificationRepository notificationRepository;
    private final ImageSimilarityScorer imageSimilarityScorer;
    private final ObjectMapper objectMapper;
    private final com.campusfind.service.EmailService emailService;

    @Value("${app.matching.threshold:50}")
    private int matchingThreshold;

    @Value("${app.matching.weights.block:25}")
    private int blockWeight;

    @Value("${app.matching.weights.room:25}")
    private int roomWeight;

    @Value("${app.matching.weights.category:20}")
    private int categoryWeight;

    @Value("${app.matching.weights.time:10}")
    private int timeWeight;

    @Value("${app.matching.weights.text:10}")
    private int textWeight;

    @Value("${app.matching.weights.image:10}")
    private int imageWeight;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
            "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just", "me",
            "more", "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
            "other", "our", "ours", "out", "over", "own", "same", "she", "should", "so", "some", "such",
            "than", "that", "the", "their", "theirs", "them", "then", "there", "these", "they", "this",
            "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what",
            "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours"
    );

    @Async
    @Transactional
    public CompletableFuture<Void> runMatchingAsync(Long itemId) {
        log.info("Starting asynchronous matching for item ID: {}", itemId);
        try {
            runMatchingForItem(itemId);
        } catch (Exception e) {
            log.error("Error during matching for item ID {}: {}", itemId, e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public List<MatchEntity> runMatchingForItem(Long itemId) {
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Item item = itemOpt.get();
        if (item.getStatus() != ItemStatus.OPEN || item.isHidden()) {
            return Collections.emptyList();
        }

        ItemType oppositeType = (item.getType() == ItemType.LOST) ? ItemType.FOUND : ItemType.LOST;
        LocalDateTime minCreatedAt = LocalDateTime.now().minusDays(30);
        Long blockId = (item.getBlock() != null) ? item.getBlock().getId() : null;

        List<Item> candidates = itemRepository.findMatchingCandidates(
                ItemStatus.OPEN,
                oppositeType,
                blockId,
                item.getCategory(),
                item.getId(),
                item.getOwner().getId(),
                minCreatedAt
        );

        List<MatchEntity> createdMatches = new ArrayList<>();

        for (Item candidate : candidates) {
            Item lostItem = (item.getType() == ItemType.LOST) ? item : candidate;
            Item foundItem = (item.getType() == ItemType.FOUND) ? item : candidate;

            // Never duplicate an existing match for the same pair
            if (matchRepository.existsByLostItemIdAndFoundItemId(lostItem.getId(), foundItem.getId())) {
                continue;
            }

            MatchScoreResult result = calculateScore(lostItem, foundItem);

            if (result.totalScore >= matchingThreshold) {
                try {
                    String breakdownJson = objectMapper.writeValueAsString(result.breakdown);

                    MatchEntity match = MatchEntity.builder()
                            .lostItem(lostItem)
                            .foundItem(foundItem)
                            .score(result.totalScore)
                            .scoreBreakdown(breakdownJson)
                            .status(MatchStatus.SUGGESTED)
                            .build();

                    MatchEntity savedMatch = matchRepository.save(match);
                    createdMatches.add(savedMatch);

                    // Notify lost item owner
                    notificationRepository.save(Notification.builder()
                            .user(lostItem.getOwner())
                            .title("Possible Match Found!")
                            .body("Possible match found for your item (score " + result.totalScore + "%)")
                            .link("/matches/" + savedMatch.getId())
                            .read(false)
                            .build());

                    // Notify found item owner
                    notificationRepository.save(Notification.builder()
                            .user(foundItem.getOwner())
                            .title("Possible Match Found!")
                            .body("Possible match found for your item (score " + result.totalScore + "%)")
                            .link("/matches/" + savedMatch.getId())
                            .read(false)
                            .build());

                    emailService.sendMatchNotification(lostItem.getOwner(), lostItem.getTitle(), savedMatch.getId());
                    emailService.sendMatchNotification(foundItem.getOwner(), foundItem.getTitle(), savedMatch.getId());

                    log.info("Created match between lost item {} and found item {} with score {}",
                            lostItem.getId(), foundItem.getId(), result.totalScore);
                } catch (Exception e) {
                    log.error("Failed to persist match between items {} and {}: {}",
                            lostItem.getId(), foundItem.getId(), e.getMessage());
                }
            }
        }

        return createdMatches;
    }

    public static class MatchScoreResult {
        public final int totalScore;
        public final Map<String, Object> breakdown;

        public MatchScoreResult(int totalScore, Map<String, Object> breakdown) {
            this.totalScore = totalScore;
            this.breakdown = breakdown;
        }
    }

    public MatchScoreResult calculateScore(Item item1, Item item2) {
        int scoreBlock = 0;
        int scoreRoom = 0;
        int scoreCategory = 0;
        int scoreTime = 0;
        int scoreText = 0;
        int scoreImage = 0;

        // 1. Same block (25 pts)
        if (item1.getBlock() != null && item2.getBlock() != null
                && Objects.equals(item1.getBlock().getId(), item2.getBlock().getId())) {
            scoreBlock = blockWeight;
        }

        // 2. Same room (25 pts)
        if (item1.getRoom() != null && item2.getRoom() != null
                && Objects.equals(item1.getRoom().getId(), item2.getRoom().getId())) {
            scoreRoom = roomWeight;
        }

        // 3. Same category (20 pts)
        if (item1.getCategory() == item2.getCategory()) {
            scoreCategory = categoryWeight;
        }

        // 4. Event time difference (up to 10 pts)
        if (item1.getEventTime() != null && item2.getEventTime() != null) {
            long hoursDiff = Math.abs(Duration.between(item1.getEventTime(), item2.getEventTime()).toHours());
            if (hoursDiff <= 3) {
                scoreTime = timeWeight;
            } else if (hoursDiff <= 24) {
                scoreTime = timeWeight / 2;
            }
        }

        // 5. Text similarity of title plus description (up to 10 pts)
        scoreText = computeTextScore(
                item1.getTitle() + " " + (item1.getDescription() != null ? item1.getDescription() : ""),
                item2.getTitle() + " " + (item2.getDescription() != null ? item2.getDescription() : "")
        );

        // 6. Image similarity (up to 10 pts)
        if (item1.getImageUrl() != null && item2.getImageUrl() != null) {
            double imgSim = imageSimilarityScorer.computeSimilarity(item1.getImageUrl(), item2.getImageUrl());
            scoreImage = (int) Math.round(imgSim * imageWeight);
        }

        int total = scoreBlock + scoreRoom + scoreCategory + scoreTime + scoreText + scoreImage;
        total = Math.min(100, Math.max(0, total));

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("block", scoreBlock);
        breakdown.put("room", scoreRoom);
        breakdown.put("category", scoreCategory);
        breakdown.put("time", scoreTime);
        breakdown.put("text", scoreText);
        breakdown.put("image", scoreImage);
        breakdown.put("total", total);

        return new MatchScoreResult(total, breakdown);
    }

    private int computeTextScore(String text1, String text2) {
        Set<String> words1 = extractMeaningfulWords(text1);
        Set<String> words2 = extractMeaningfulWords(text2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        if (intersection.isEmpty()) {
            return 0;
        }

        // Overlap ratio relative to smaller token set
        double overlap = (double) intersection.size() / Math.min(words1.size(), words2.size());
        int score = (int) Math.round(overlap * textWeight);
        return Math.min(textWeight, score);
    }

    private Set<String> extractMeaningfulWords(String text) {
        if (text == null) {
            return Collections.emptySet();
        }

        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = cleaned.split("\\s+");
        Set<String> result = new HashSet<>();

        for (String token : tokens) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }
}
