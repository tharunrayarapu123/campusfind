package com.campusfind.controller;

import com.campusfind.dto.response.*;
import com.campusfind.service.AdminService;
import com.campusfind.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Campus administrator moderation, user governance, reports, and system statistics")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/stats")
    @Operation(summary = "Get aggregate campus system stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getStats() {
        AdminStatsDto stats = adminService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.ok("System statistics retrieved successfully", stats));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get paginated community reports")
    public ResponseEntity<ApiResponse<Page<ReportDto>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ReportDto> reports = adminService.getReports(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Reports retrieved successfully", reports));
    }

    @PostMapping("/reports/{itemId}/dismiss")
    @Operation(summary = "Dismiss all community reports for an item")
    public ResponseEntity<ApiResponse<Void>> dismissReports(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        adminService.dismissReports(itemId, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Reports dismissed successfully", null));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Hard delete an item post (with notification dispatched to owner)")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        adminService.deleteItem(id, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Item post permanently deleted", null));
    }

    @GetMapping("/users")
    @Operation(summary = "Get paginated user list with optional search query")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserResponse> users = adminService.getUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", users));
    }

    @PostMapping("/users/{id}/ban")
    @Operation(summary = "Ban a user account (prevents login)")
    public ResponseEntity<ApiResponse<UserResponse>> banUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UserResponse banned = adminService.banUser(id, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User account successfully banned", banned));
    }

    @PostMapping("/users/{id}/unban")
    @Operation(summary = "Unban a user account")
    public ResponseEntity<ApiResponse<UserResponse>> unbanUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        UserResponse unbanned = adminService.unbanUser(id, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User account successfully unbanned", unbanned));
    }

    @PostMapping("/items/{id}/unhide")
    @Operation(summary = "Unhide an item that was automatically hidden by reports")
    public ResponseEntity<ApiResponse<Void>> unhideItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        adminService.unhideItem(id, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Item post is now visible to the public", null));
    }

    @PostMapping("/matches/{matchId}/claims/{claimId}/office-approve")
    @Operation(summary = "Approve office claim proof for item handed to security/admin office")
    public ResponseEntity<ApiResponse<ClaimResponse>> approveOfficeClaim(
            @PathVariable Long matchId,
            @PathVariable Long claimId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        ClaimResponse response = adminService.officeApproveClaim(matchId, claimId, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Office claim proof approved. Match is now CONFIRMED.", response));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get system audit logs of administrative actions")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved successfully", auditLogs));
    }
}
