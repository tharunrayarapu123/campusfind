package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administration and moderation operations (ADMIN role required)")
public class AdminController {

    @GetMapping("/stats")
    @Operation(summary = "Admin statistics endpoint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        return ResponseEntity.ok(ApiResponse.success("Admin statistics retrieved", Map.of(
                "status", "HEALTHY",
                "roleRequired", "ADMIN"
        )));
    }
}
