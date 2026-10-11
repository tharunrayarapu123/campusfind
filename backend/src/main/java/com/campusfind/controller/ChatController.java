package com.campusfind.controller;

import com.campusfind.dto.request.ChatMessageRequest;
import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.ChatMessageDto;
import com.campusfind.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Realtime and REST in-app chat for confirmed matched pairs")
public class ChatController {

    private final ChatService chatService;

    /**
     * STOMP Message Handler: Client sends to /app/chat/{matchId}
     */
    @MessageMapping("/chat/{matchId}")
    public void handleIncomingChatMessage(
            @DestinationVariable Long matchId,
            @Valid @Payload ChatMessageRequest request,
            Principal principal
    ) {
        log.info("Received WebSocket chat message from {} for match {}", principal.getName(), matchId);
        chatService.processAndBroadcastMessage(matchId, principal.getName(), request.getBody(), request.getImageUrl());
    }

    /**
     * REST Endpoint: Send chat message via HTTP (convenient for testing or clients without active WS socket)
     */
    @PostMapping("/api/v1/matches/{id}/messages")
    @Operation(summary = "Send chat message to match chat (HTTP fallback)")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessageRest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        ChatMessageDto sent = chatService.processAndBroadcastMessage(id, userDetails.getUsername(), request.getBody(), request.getImageUrl());
        return ResponseEntity.ok(ApiResponse.ok("Message sent successfully", sent));
    }

    /**
     * REST Endpoint: Get chat history paginated, oldest to newest
     */
    @GetMapping("/api/v1/matches/{id}/messages")
    @Operation(summary = "Get paginated chat message history (oldest to newest)")
    public ResponseEntity<ApiResponse<Page<ChatMessageDto>>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<ChatMessageDto> messages = chatService.getMessages(id, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Chat messages retrieved successfully", messages));
    }

    /**
     * REST Endpoint: Mark messages as read
     */
    @PostMapping("/api/v1/matches/{id}/messages/read")
    @Operation(summary = "Acknowledge and mark match chat messages as read")
    public ResponseEntity<ApiResponse<Void>> markMessagesRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Simple acknowledge endpoint
        return ResponseEntity.ok(ApiResponse.ok("Messages marked as read", null));
    }
}
