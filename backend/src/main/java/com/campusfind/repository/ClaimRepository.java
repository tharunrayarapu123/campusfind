package com.campusfind.repository;

import com.campusfind.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByMatchId(Long matchId);
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);
}
