package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.MatchResponseDto;
import com.campusfind.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Smart item matching engine results and rejection")
public class MatchController {

    private final MatchService matchService;
    private final com.campusfind.service.ClaimService claimService;

    @GetMapping
    @Operation(summary = "Get current user's matches (paginated, privacy-safe)")
    public ResponseEntity<ApiResponse<Page<MatchResponseDto>>> getMyMatches(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MatchResponseDto> matches = matchService.getMyMatches(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match by ID (restricted strictly to the two matched owners or ADMIN)")
    public ResponseEntity<ApiResponse<MatchResponseDto>> getMatchById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MatchResponseDto match = matchService.getMatchById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(match));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a suggested match so it is not suggested again")
    public ResponseEntity<ApiResponse<MatchResponseDto>> rejectMatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MatchResponseDto updated = matchService.rejectMatch(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Match rejected successfully", updated));
    }

    @GetMapping("/{id}/verification")
    @Operation(summary = "Get verification question for the match (lost item owner)")
    public ResponseEntity<ApiResponse<com.campusfind.dto.response.VerificationQuestionResponse>> getVerificationQuestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var response = claimService.getVerificationQuestion(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Verification question retrieved", response));
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "Submit an answer to the verification question (max 3 attempts)")
    public ResponseEntity<ApiResponse<com.campusfind.dto.response.ClaimResponse>> submitClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @jakarta.validation.Valid @RequestBody com.campusfind.dto.request.ClaimRequest request
    ) {
        var response = claimService.submitClaim(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Claim answer submitted successfully", response));
    }

    @GetMapping("/{id}/claims")
    @Operation(summary = "Get submitted claims for this match")
    public ResponseEntity<ApiResponse<java.util.List<com.campusfind.dto.response.ClaimResponse>>> getClaims(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var response = claimService.getClaimsForMatch(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Claims retrieved successfully", response));
    }

    @PostMapping("/{id}/claims/{claimId}/approve")
    @Operation(summary = "Approve claim answer (finder only)")
    public ResponseEntity<ApiResponse<com.campusfind.dto.response.ClaimResponse>> approveClaim(
            @PathVariable Long id,
            @PathVariable Long claimId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var response = claimService.approveClaim(id, claimId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Claim approved successfully. Chat is now unlocked.", response));
    }

    @PostMapping("/{id}/claims/{claimId}/reject")
    @Operation(summary = "Reject claim answer (finder only)")
    public ResponseEntity<ApiResponse<com.campusfind.dto.response.ClaimResponse>> rejectClaim(
            @PathVariable Long id,
            @PathVariable Long claimId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var response = claimService.rejectClaim(id, claimId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Claim rejected successfully", response));
    }

    @PostMapping("/{id}/claim-office")
    @Operation(summary = "Submit office pickup claim proof when item was handed to office")
    public ResponseEntity<ApiResponse<com.campusfind.dto.response.ClaimResponse>> submitOfficeClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @jakarta.validation.Valid @RequestBody com.campusfind.dto.request.OfficeClaimRequest request
    ) {
        var response = claimService.submitOfficeClaim(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Office claim proof submitted successfully", response));
    }

    @PostMapping("/{id}/returned")
    @Operation(summary = "Mark match as returned (either owner; awards +10 karma to finder)")
    public ResponseEntity<ApiResponse<MatchResponseDto>> markReturned(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var response = matchService.markReturned(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Item marked as returned. +10 karma points awarded to finder.", response));
    }
}
