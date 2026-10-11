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

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /**
     * Paginated item feed with optional filters:
     * type, category, block, room, status, and keyword search across title and description.
     */
    @Query("SELECT i FROM Item i WHERE " +
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
            Pageable pageable
    );

    /**
     * Open candidate items of the opposite type with either the same block or the same category for matching.
     */
    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.type = :oppositeType AND " +
           "i.id <> :excludeItemId AND " +
           "((:blockId IS NOT NULL AND i.block IS NOT NULL AND i.block.id = :blockId) OR i.category = :category)")
    List<Item> findMatchingCandidates(
            @Param("status") ItemStatus status,
            @Param("oppositeType") ItemType oppositeType,
            @Param("blockId") Long blockId,
            @Param("category") ItemCategory category,
            @Param("excludeItemId") Long excludeItemId
    );

    /**
     * Admin stats counts
     */
    long countByType(ItemType type);

    long countByStatus(ItemStatus status);

    List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
