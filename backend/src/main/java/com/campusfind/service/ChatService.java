package com.campusfind.service;

import com.campusfind.dto.response.ChatMessageDto;
import com.campusfind.entity.ChatMessage;
import com.campusfind.entity.MatchEntity;
import com.campusfind.entity.MatchStatus;
import com.campusfind.entity.User;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.ChatMessageRepository;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // Rate limiter: Max 30 messages per minute per user
    private final Map<Long, ConcurrentLinkedQueue<Instant>> userMessageTimestamps = new ConcurrentHashMap<>();

    @Transactional
    public ChatMessageDto processAndBroadcastMessage(Long matchId, String senderEmail, String body, String imageUrl) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", senderEmail));

        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        validateChatSendAccess(match, sender);

        // Body validation
        if (body == null || body.trim().isEmpty()) {
            throw new ApiException("Message body cannot be empty", HttpStatus.BAD_REQUEST);
        }
        String trimmedBody = body.trim();
        if (trimmedBody.length() > 1000) {
            throw new ApiException("Message body exceeds 1000 characters", HttpStatus.BAD_REQUEST);
        }

        // Rate limit check: max 30 per minute
        enforceRateLimit(sender.getId());

        // Save message in database
        ChatMessage message = ChatMessage.builder()
                .match(match)
                .sender(sender)
                .body(trimmedBody)
                .imageUrl(imageUrl != null && !imageUrl.isBlank() ? imageUrl.trim() : null)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageDto dto = toDto(saved);

        // Broadcast to WebSocket subscribers: /topic/match/{matchId}
        messagingTemplate.convertAndSend("/topic/match/" + matchId, dto);
        log.debug("Broadcasted message ID {} to /topic/match/{}", saved.getId(), matchId);

        // Notify other user if no notification was sent for this match within the last 5 minutes
        User otherUser = match.getLostItem().getOwner().getId().equals(sender.getId())
                ? match.getFoundItem().getOwner()
                : match.getLostItem().getOwner();

        String chatLink = "/matches/" + matchId + "/chat";
        if (!notificationService.hasRecentNotificationForLink(otherUser.getId(), chatLink, 5)) {
            notificationService.createNotification(
                    otherUser,
                    "New Message",
                    sender.getFullName() + " sent you a message about matched item '" + match.getFoundItem().getTitle() + "'",
                    chatLink
            );
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessages(Long matchId, String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        validateChatReadAccess(match, user);

        return chatMessageRepository.findByMatchIdOrderByCreatedAtAsc(matchId, pageable)
                .map(this::toDto);
    }

    public void validateChatSendAccess(MatchEntity match, User user) {
        boolean isLostOwner = match.getLostItem().getOwner().getId().equals(user.getId());
        boolean isFoundOwner = match.getFoundItem().getOwner().getId().equals(user.getId());

        if (!isLostOwner && !isFoundOwner) {
            throw new AccessDeniedException("Access denied: You are not a participant in this match");
        }

        if (match.getStatus() == MatchStatus.RETURNED) {
            throw new ApiException("Chat is read-only after item is returned", HttpStatus.BAD_REQUEST);
        }

        if (match.getStatus() != MatchStatus.CONFIRMED) {
            throw new ApiException("Chat is only available for CONFIRMED matches (current status: " + match.getStatus() + ")", HttpStatus.BAD_REQUEST);
        }
    }

    public void validateChatReadAccess(MatchEntity match, User user) {
        boolean isLostOwner = match.getLostItem().getOwner().getId().equals(user.getId());
        boolean isFoundOwner = match.getFoundItem().getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == com.campusfind.entity.Role.ADMIN;

        if (!isLostOwner && !isFoundOwner && !isAdmin) {
            throw new AccessDeniedException("Access denied: You are not authorized to view messages for this match");
        }

        if (match.getStatus() != MatchStatus.CONFIRMED && match.getStatus() != MatchStatus.RETURNED) {
            throw new ApiException("Chat history is only accessible once a match is CONFIRMED or RETURNED", HttpStatus.BAD_REQUEST);
        }
    }

    private void enforceRateLimit(Long userId) {
        ConcurrentLinkedQueue<Instant> timestamps = userMessageTimestamps.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);

        while (!timestamps.isEmpty() && timestamps.peek().isBefore(oneMinuteAgo)) {
            timestamps.poll();
        }

        if (timestamps.size() >= 30) {
            throw new ApiException("Rate limit exceeded: You may send at most 30 messages per minute", HttpStatus.TOO_MANY_REQUESTS);
        }

        timestamps.add(Instant.now());
    }

    public ChatMessageDto toDto(ChatMessage message) {
        // Privacy rule: sender is exposed as PublicUserDto only (no email or phone)
        return ChatMessageDto.builder()
                .id(message.getId())
                .matchId(message.getMatch().getId())
                .sender(ItemService.toPublicUserDto(message.getSender()))
                .body(message.getBody())
                .imageUrl(message.getImageUrl())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
