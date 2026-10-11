package com.campusfind.security;

import com.campusfind.entity.MatchEntity;
import com.campusfind.entity.MatchStatus;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(message, accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT rejected: Missing or invalid Authorization header");
            throw new MessageDeliveryException("Unauthorized: Missing or invalid JWT Authorization header in CONNECT frame");
        }

        String token = authHeader.substring(7);
        try {
            String userEmail = jwtService.extractUsername(token);
            if (userEmail == null) {
                throw new MessageDeliveryException("Unauthorized: Invalid JWT token claims");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new MessageDeliveryException("Unauthorized: Token is expired or invalid");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            if (accessor.isMutable()) {
                accessor.setUser(authentication);
                log.info("STOMP connection authenticated for user: {}", userEmail);
                return message;
            } else {
                StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
                mutableAccessor.setUser(authentication);
                log.info("STOMP connection authenticated for user: {}", userEmail);
                return org.springframework.messaging.support.MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
            }
        } catch (MessageDeliveryException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to authenticate STOMP connection: {}", ex.getMessage());
            throw new MessageDeliveryException("Unauthorized: " + ex.getMessage());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/topic/match/")) {
            Long matchId = extractIdFromDestination(destination, "/topic/match/");
            User user = getAuthenticatedUser(accessor.getUser());

            MatchEntity match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new MessageDeliveryException("Match not found: " + matchId));

            boolean isLostOwner = match.getLostItem().getOwner().getId().equals(user.getId());
            boolean isFoundOwner = match.getFoundItem().getOwner().getId().equals(user.getId());
            boolean isAdmin = user.getRole() == Role.ADMIN;

            if (!isLostOwner && !isFoundOwner && !isAdmin) {
                log.warn("User {} denied subscription to /topic/match/{}", user.getEmail(), matchId);
                throw new MessageDeliveryException("Forbidden: Not authorized to subscribe to match " + matchId);
            }

            if (match.getStatus() != MatchStatus.CONFIRMED && match.getStatus() != MatchStatus.RETURNED) {
                log.warn("Subscription to match {} denied because status is {}", matchId, match.getStatus());
                throw new MessageDeliveryException("Forbidden: Chat subscription is only allowed for CONFIRMED or RETURNED matches");
            }
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/app/chat/")) {
            Long matchId = extractIdFromDestination(destination, "/app/chat/");
            User user = getAuthenticatedUser(accessor.getUser());

            MatchEntity match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new MessageDeliveryException("Match not found: " + matchId));

            boolean isLostOwner = match.getLostItem().getOwner().getId().equals(user.getId());
            boolean isFoundOwner = match.getFoundItem().getOwner().getId().equals(user.getId());

            if (!isLostOwner && !isFoundOwner) {
                log.warn("User {} denied sending to /app/chat/{}", user.getEmail(), matchId);
                throw new MessageDeliveryException("Forbidden: Not authorized to send messages to match " + matchId);
            }

            if (match.getStatus() == MatchStatus.RETURNED) {
                throw new MessageDeliveryException("Forbidden: Chat is read-only after item is marked RETURNED");
            }

            if (match.getStatus() != MatchStatus.CONFIRMED) {
                throw new MessageDeliveryException("Forbidden: Chat is only allowed for CONFIRMED matches");
            }
        }
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new MessageDeliveryException("Unauthorized: No authenticated user found for this WebSocket session");
        }
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MessageDeliveryException("Authenticated user not found: " + email));
    }

    private Long extractIdFromDestination(String destination, String prefix) {
        try {
            String idStr = destination.substring(prefix.length());
            int slashIndex = idStr.indexOf('/');
            if (slashIndex != -1) {
                idStr = idStr.substring(0, slashIndex);
            }
            return Long.parseLong(idStr);
        } catch (Exception ex) {
            throw new MessageDeliveryException("Invalid destination format: " + destination);
        }
    }
}
