package com.campusfind.repository;

import com.campusfind.entity.Item;
import com.campusfind.entity.ItemCategory;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /**
     * Paginated item feed with optional filters, ownership filter (mine=true),
     * keyword search, and hidden suppression for non-admin feeds.
     */
    @Query("SELECT i FROM Item i WHERE " +
           "(:includeHidden = true OR i.hidden = false) AND " +
           "(:ownerId IS NULL OR i.owner.id = :ownerId) AND " +
           "(:type IS NULL OR i.type = :type) AND " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(:blockId IS NULL OR (i.block IS NOT NULL AND i.block.id = :blockId)) AND " +
           "(:roomId IS NULL OR (i.room IS NOT NULL AND i.room.id = :roomId)) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR (i.description IS NOT NULL AND LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Item> findItemsWithFilters(
            @Param("type") ItemType type,
            @Param("category") ItemCategory category,
            @Param("blockId") Long blockId,
            @Param("roomId") Long roomId,
            @Param("status") ItemStatus status,
            @Param("keyword") String keyword,
            @Param("ownerId") Long ownerId,
            @Param("includeHidden") boolean includeHidden,
            Pageable pageable
    );

    /**
     * Open candidate items of opposite type within 30 days, same block or same category,
     * owned by a different user, and not hidden.
     */
    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.type = :oppositeType AND " +
           "i.hidden = false AND " +
           "i.owner.id <> :ownerId AND " +
           "i.createdAt >= :minCreatedAt AND " +
           "i.id <> :excludeItemId AND " +
           "((:blockId IS NOT NULL AND i.block IS NOT NULL AND i.block.id = :blockId) OR i.category = :category)")
    List<Item> findMatchingCandidates(
            @Param("status") ItemStatus status,
            @Param("oppositeType") ItemType oppositeType,
            @Param("blockId") Long blockId,
            @Param("category") ItemCategory category,
            @Param("excludeItemId") Long excludeItemId,
            @Param("ownerId") Long ownerId,
            @Param("minCreatedAt") LocalDateTime minCreatedAt
    );

    /**
     * Daily post rate limit check: max 10 new items per user per 24 hours
     */
    long countByOwnerIdAndCreatedAtAfter(Long ownerId, LocalDateTime after);

    /**
     * Auto-close scheduled job: items OPEN for more than 30 days
     */
    List<Item> findByStatusAndCreatedAtBefore(ItemStatus status, LocalDateTime threshold);

    /**
     * Re-run matching safety net: items created recently with no matches yet
     */
    @Query("SELECT i FROM Item i WHERE i.createdAt >= :after AND i.status = 'OPEN' AND i.hidden = false AND " +
           "NOT EXISTS (SELECT m FROM MatchEntity m WHERE m.lostItem.id = i.id OR m.foundItem.id = i.id)")
    List<Item> findRecentItemsWithoutMatches(@Param("after") LocalDateTime after);

    /**
     * Admin stats counts
     */
    long countByType(ItemType type);

    long countByStatus(ItemStatus status);

    List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
