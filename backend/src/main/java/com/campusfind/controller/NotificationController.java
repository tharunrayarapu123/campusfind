package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.NotificationDto;
import com.campusfind.dto.response.UnreadCountDto;
import com.campusfind.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<NotificationDto> notifications = notificationService.getUserNotifications(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Unread notification count retrieved", new UnreadCountDto(count)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        NotificationDto updated = notificationService.markAsRead(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", updated));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }
}
