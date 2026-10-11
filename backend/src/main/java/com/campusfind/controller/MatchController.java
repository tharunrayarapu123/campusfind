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
}
