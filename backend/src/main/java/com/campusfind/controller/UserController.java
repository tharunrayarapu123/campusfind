package com.campusfind.controller;

import com.campusfind.dto.request.ChangePasswordRequest;
import com.campusfind.dto.request.UpdateUserRequest;
import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.PublicUserDto;
import com.campusfind.dto.response.UserResponse;
import com.campusfind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management and privacy-safe lookups")
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse updated = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/me/stats")
    @Operation(summary = "Get current user activity stats, karma, and badges")
    public ResponseEntity<ApiResponse<com.campusfind.dto.response.UserStatsDto>> getMyStats(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var stats = userService.getUserStats(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User stats retrieved successfully", stats));
    }

    @PutMapping("/me/settings")
    @Operation(summary = "Update user notification settings")
    public ResponseEntity<ApiResponse<UserResponse>> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.campusfind.dto.request.UpdateSettingsRequest request
    ) {
        UserResponse response = userService.updateSettings(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Settings updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch another user public profile (privacy rule: phone and email omitted)")
    public ResponseEntity<ApiResponse<PublicUserDto>> getPublicUser(@PathVariable Long id) {
        PublicUserDto publicUser = userService.getPublicUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(publicUser));
    }
}
