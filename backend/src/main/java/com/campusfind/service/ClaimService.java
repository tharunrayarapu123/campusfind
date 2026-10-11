package com.campusfind.service;

import com.campusfind.dto.request.ClaimRequest;
import com.campusfind.dto.request.OfficeClaimRequest;
import com.campusfind.dto.response.ClaimResponse;
import com.campusfind.dto.response.VerificationQuestionResponse;
import com.campusfind.entity.*;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.ClaimRepository;
import com.campusfind.repository.ItemRepository;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final MatchRepository matchRepository;
    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public VerificationQuestionResponse getVerificationQuestion(Long matchId, String userEmail) {
        MatchEntity match = getMatchOrThrow(matchId);
        User user = getUserOrThrow(userEmail);

        validateParticipant(match, user);

        Item foundItem = match.getFoundItem();
        if (foundItem.isHandedToOffice()) {
            return VerificationQuestionResponse.builder()
                    .matchId(matchId)
                    .handedToOffice(true)
                    .verificationQuestion(null)
                    .instructions("This item was deposited at the campus security/admin office. Submit an office claim with proof of ownership for administrator approval.")
                    .build();
        }

        return VerificationQuestionResponse.builder()
                .matchId(matchId)
                .handedToOffice(false)
                .verificationQuestion(foundItem.getVerificationQuestion())
                .instructions("Please answer the finder's verification question truthfully to confirm ownership.")
                .build();
    }

    @Transactional
    public ClaimResponse submitClaim(Long matchId, String userEmail, ClaimRequest request) {
        MatchEntity match = getMatchOrThrow(matchId);
        User user = getUserOrThrow(userEmail);

        // Only the lost-item owner can submit a claim
        if (!match.getLostItem().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the owner of the lost item can submit a claim answer");
        }

        if (match.getStatus() == MatchStatus.RETURNED || match.getStatus() == MatchStatus.REJECTED) {
            throw new ApiException("Cannot submit claim for a match that is " + match.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Max 3 attempts per match
        long attempts = claimRepository.countByMatchIdAndClaimantId(matchId, user.getId());
        if (attempts >= 3) {
            throw new ApiException("Maximum claim attempts (3) exceeded for this match", HttpStatus.TOO_MANY_REQUESTS);
        }

        Claim claim = Claim.builder()
                .match(match)
                .claimant(user)
                .answer(request.getAnswer().trim())
                .officeClaim(false)
                .build();

        Claim savedClaim = claimRepository.save(claim);

        // Notify finder
        User finder = match.getFoundItem().getOwner();
        notificationService.createNotification(
                finder,
                "New Claim Submitted",
                user.getFullName() + " submitted an answer for your found item '" + match.getFoundItem().getTitle() + "'",
                "/matches/" + matchId
        );
        emailService.sendClaimSubmittedNotification(finder, match.getFoundItem().getTitle(), matchId);

        return toClaimResponse(savedClaim);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getClaimsForMatch(Long matchId, String userEmail) {
        MatchEntity match = getMatchOrThrow(matchId);
        User user = getUserOrThrow(userEmail);

        validateParticipant(match, user);

        return claimRepository.findByMatchIdOrderByCreatedAtDesc(matchId).stream()
                .map(this::toClaimResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClaimResponse approveClaim(Long matchId, Long claimId, String userEmail) {
        MatchEntity match = getMatchOrThrow(matchId);
        User user = getUserOrThrow(userEmail);

        // Only the finder can approve
        if (!match.getFoundItem().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the finder can approve a claim for this found item");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        if (!claim.getMatch().getId().equals(matchId)) {
            throw new ApiException("Claim does not belong to the specified match", HttpStatus.BAD_REQUEST);
        }

        claim.setApproved(true);
        Claim updatedClaim = claimRepository.save(claim);

        // Transition match status to CONFIRMED
        match.setStatus(MatchStatus.CONFIRMED);
        matchRepository.save(match);

        // Transition both items to MATCHED
        Item lost = match.getLostItem();
        Item found = match.getFoundItem();
        lost.setStatus(ItemStatus.MATCHED);
        found.setStatus(ItemStatus.MATCHED);
        itemRepository.save(lost);
        itemRepository.save(found);

        // Reject all other SUGGESTED matches of these two items
        List<MatchEntity> others = matchRepository.findOtherSuggestedMatches(lost.getId(), found.getId(), matchId);
        for (MatchEntity other : others) {
            other.setStatus(MatchStatus.REJECTED);
        }
        if (!others.isEmpty()) {
            matchRepository.saveAll(others);
            log.info("Auto-rejected {} other suggested matches for items {} and {}", others.size(), lost.getId(), found.getId());
        }

        // Notify claimant (lost-item owner)
        User owner = lost.getOwner();
        notificationService.createNotification(
                owner,
                "Claim Approved!",
                "Your ownership claim for '" + found.getTitle() + "' was approved! Private chat is now unlocked.",
                "/matches/" + matchId
        );
        emailService.sendClaimDecisionNotification(owner, found.getTitle(), true, matchId);

        return toClaimResponse(updatedClaim);
    }

    @Transactional
    public ClaimResponse rejectClaim(Long matchId, Long claimId, String userEmail) {
        MatchEntity match = getMatchOrThrow(matchId);
        User user = getUserOrThrow(userEmail);

        // Only the finder can reject
        if (!match.getFoundItem().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the finder can reject a claim for this found item");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        if (!claim.getMatch().getId().equals(matchId)) {
            throw new ApiException("Claim does not belong to the specified match", HttpStatus.BAD_REQUEST);
        }

        claim.setApproved(false);
        Claim updatedClaim = claimRepository.save(claim);

        // Notify claimant
        User owner = match.getLostItem().getOwner();
        notificationService.createNotification(
                owner,
                "Claim Rejected",
                "Your claim answer for '" + match.getFoundItem().getTitle() + "' was not accepted by the finder.",
                "/matches/" + matchId
        );
        emailService.sendClaimDecisionNotification(owner, match.getFoundItem().getTitle(), false, matchId);

        return toClaimResponse(updatedClaim);
    }

    @Transactional
    public ClaimResponse submitOfficeClaim(Long matchId, String userEmail, OfficeClaimRequest request) {
        MatchEntity match = getMatchOrThrow(matchId);
        User user = getUserOrThrow(userEmail);

        // Only the lost-item owner can submit
        if (!match.getLostItem().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the owner of the lost item can submit an office claim");
        }

        if (!match.getFoundItem().isHandedToOffice()) {
            throw new ApiException("This item was not handed over to an office. Use standard claim verification.", HttpStatus.BAD_REQUEST);
        }

        Claim claim = Claim.builder()
                .match(match)
                .claimant(user)
                .answer("OFFICE PROOF: " + request.getDescriptionProof().trim())
                .officeClaim(true)
                .build();

        Claim savedClaim = claimRepository.save(claim);

        notificationService.createNotification(
                user,
                "Office Claim Submitted",
                "Your office proof for '" + match.getFoundItem().getTitle() + "' was submitted and is pending administrator review.",
                "/matches/" + matchId
        );

        return toClaimResponse(savedClaim);
    }

    @Transactional
    public ClaimResponse approveOfficeClaim(Long matchId, Long claimId, String adminEmail) {
        MatchEntity match = getMatchOrThrow(matchId);
        User admin = getUserOrThrow(adminEmail);

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only campus administrators can approve office claims");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        claim.setApproved(true);
        Claim updatedClaim = claimRepository.save(claim);

        // Match becomes CONFIRMED
        match.setStatus(MatchStatus.CONFIRMED);
        matchRepository.save(match);

        Item lost = match.getLostItem();
        Item found = match.getFoundItem();
        lost.setStatus(ItemStatus.MATCHED);
        found.setStatus(ItemStatus.MATCHED);
        itemRepository.save(lost);
        itemRepository.save(found);

        // Reject other suggested matches
        List<MatchEntity> others = matchRepository.findOtherSuggestedMatches(lost.getId(), found.getId(), matchId);
        for (MatchEntity other : others) {
            other.setStatus(MatchStatus.REJECTED);
        }
        if (!others.isEmpty()) {
            matchRepository.saveAll(others);
        }

        auditLogService.logAction(
                admin,
                "OFFICE_APPROVE_CLAIM",
                "MATCH",
                matchId,
                "Admin approved office drop point claim ID " + claimId
        );

        notificationService.createNotification(
                lost.getOwner(),
                "Office Claim Approved",
                "Campus office approved your ownership proof for '" + found.getTitle() + "'. You may pick it up at the office.",
                "/matches/" + matchId
        );
        emailService.sendClaimDecisionNotification(lost.getOwner(), found.getTitle(), true, matchId);

        return toClaimResponse(updatedClaim);
    }

    private MatchEntity getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateParticipant(MatchEntity match, User user) {
        boolean isLostOwner = match.getLostItem().getOwner().getId().equals(user.getId());
        boolean isFoundOwner = match.getFoundItem().getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isLostOwner && !isFoundOwner && !isAdmin) {
            throw new AccessDeniedException("Access denied: You are not a participant in this match");
        }
    }

    public ClaimResponse toClaimResponse(Claim claim) {
        return ClaimResponse.builder()
                .id(claim.getId())
                .matchId(claim.getMatch().getId())
                .claimant(ItemService.toPublicUserDto(claim.getClaimant()))
                .answer(claim.getAnswer())
                .approved(claim.getApproved())
                .officeClaim(claim.isOfficeClaim())
                .createdAt(claim.getCreatedAt())
                .build();
    }
}
