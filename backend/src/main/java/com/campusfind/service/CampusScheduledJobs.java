package com.campusfind.service;

import com.campusfind.entity.Item;
import com.campusfind.entity.ItemStatus;
import com.campusfind.repository.ItemRepository;
import com.campusfind.service.matching.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampusScheduledJobs {

    private final ItemRepository itemRepository;
    private final MatchingService matchingService;

    /**
     * Runs every night at 02:00 AM UTC:
     * Sets all OPEN items created more than 30 days ago to CLOSED.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoCloseExpiredItems() {
        log.info("Running nightly cleanup: Closing OPEN items older than 30 days...");
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Item> expiredItems = itemRepository.findByStatusAndCreatedAtBefore(ItemStatus.OPEN, thirtyDaysAgo);

        for (Item item : expiredItems) {
            item.setStatus(ItemStatus.CLOSED);
            itemRepository.save(item);
        }

        log.info("Nightly cleanup complete: Closed {} expired items.", expiredItems.size());
    }

    /**
     * Runs every 10 minutes:
     * Safety net re-running matching for items created in the last hour that have no matches yet.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes (600,000 ms)
    public void retryUnmatchedRecentItems() {
        log.debug("Running matching safety net for recent items without matches...");
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Item> recentUnmatched = itemRepository.findRecentItemsWithoutMatches(oneHourAgo);

        for (Item item : recentUnmatched) {
            try {
                matchingService.runMatchingForItem(item.getId());
            } catch (Exception e) {
                log.warn("Matching safety net execution failed for item ID {}: {}", item.getId(), e.getMessage());
            }
        }
    }
}
