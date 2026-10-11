package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.BlockWithRoomsDto;
import com.campusfind.dto.response.CategoryDto;
import com.campusfind.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "Campus blocks, rooms, and item category lookups")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/blocks")
    @Operation(summary = "Get all campus blocks with their rooms (cached)")
    public ResponseEntity<ApiResponse<List<BlockWithRoomsDto>>> getBlocks() {
        List<BlockWithRoomsDto> blocks = referenceDataService.getBlocksWithRooms();
        return ResponseEntity.ok(ApiResponse.success(blocks));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all item categories with display names (cached)")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        List<CategoryDto> categories = referenceDataService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
