package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.LeaderboardUserDto;
import com.campusfind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Community leaderboard rankings and top karma holders")
public class LeaderboardController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get top 20 users ranked by karma points and badge achievements")
    public ResponseEntity<ApiResponse<List<LeaderboardUserDto>>> getLeaderboard() {
        List<LeaderboardUserDto> leaderboard = userService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.ok("Leaderboard retrieved successfully", leaderboard));
    }
}
