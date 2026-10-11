package com.campusfind.repository;

import com.campusfind.entity.MatchEntity;
import com.campusfind.entity.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    @Query("SELECT m FROM MatchEntity m " +
           "WHERE m.lostItem.owner.id = :userId OR m.foundItem.owner.id = :userId " +
           "ORDER BY m.createdAt DESC")
    List<MatchEntity> findMatchesByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM MatchEntity m " +
           "WHERE (m.lostItem.owner.id = :userId OR m.foundItem.owner.id = :userId) " +
           "ORDER BY m.createdAt DESC")
    Page<MatchEntity> findUserMatchesPaginated(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m FROM MatchEntity m " +
           "WHERE (m.lostItem.id = :itemId OR m.foundItem.id = :itemId) AND m.status <> 'REJECTED' " +
           "ORDER BY m.score DESC")
    List<MatchEntity> findActiveMatchesByItemId(@Param("itemId") Long itemId);

    Optional<MatchEntity> findByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);

    boolean existsByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
}
