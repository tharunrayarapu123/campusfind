package com.campusfind.controller;

import com.campusfind.dto.request.CreateItemRequest;
import com.campusfind.dto.request.ReportRequest;
import com.campusfind.dto.request.UpdateItemRequest;
import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.ItemDetailDto;
import com.campusfind.dto.response.ItemSummaryDto;
import com.campusfind.dto.response.MatchResponseDto;
import com.campusfind.entity.ItemCategory;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.ItemType;
import com.campusfind.service.ItemService;
import com.campusfind.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Lost and Found item postings and management")
public class ItemController {

    private final ItemService itemService;
    private final MatchService matchService;

    @PostMapping
    @Operation(summary = "Post new Lost or Found item")
    public ResponseEntity<ApiResponse<ItemDetailDto>> createItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateItemRequest request
    ) {
        ItemDetailDto created = itemService.createItem(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item posted successfully", created));
    }

    @GetMapping
    @Operation(summary = "Get paginated items feed with filters")
    public ResponseEntity<ApiResponse<Page<ItemSummaryDto>>> getItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) Long blockId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") Boolean mine,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String email = (userDetails != null) ? userDetails.getUsername() : null;
        Page<ItemSummaryDto> items = itemService.getItems(
                email, type, category, blockId, roomId, status, keyword, mine, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item details by ID")
    public ResponseEntity<ApiResponse<ItemDetailDto>> getItemById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = (userDetails != null) ? userDetails.getUsername() : null;
        ItemDetailDto item = itemService.getItemById(id, email);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an item (owner or ADMIN only)")
    public ResponseEntity<ApiResponse<ItemDetailDto>> updateItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        ItemDetailDto updated = itemService.updateItem(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Item updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an item (owner or ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        itemService.deleteItem(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Item deleted successfully", null));
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Report an item for moderation")
    public ResponseEntity<ApiResponse<Void>> reportItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReportRequest request
    ) {
        itemService.reportItem(id, userDetails.getUsername(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Item reported successfully", null));
    }

    @GetMapping("/{id}/matches")
    @Operation(summary = "Get suggested matches for a specific item (owner or ADMIN only)")
    public ResponseEntity<ApiResponse<List<MatchResponseDto>>> getMatchesForItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<MatchResponseDto> matches = matchService.getMatchesForItem(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(matches));
    }
}
