package com.campusfind.repository;

import com.campusfind.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByMatchIdOrderByCreatedAtAsc(Long matchId);
    List<Claim> findByMatchIdOrderByCreatedAtDesc(Long matchId);
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);
    long countByMatchIdAndClaimantId(Long matchId, Long claimantId);
    long countByMatchId(Long matchId);
    Optional<Claim> findTopByMatchIdOrderByCreatedAtDesc(Long matchId);
}
