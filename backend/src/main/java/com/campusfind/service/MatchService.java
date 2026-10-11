package com.campusfind.service;

import com.campusfind.dto.response.MatchResponseDto;
import com.campusfind.dto.response.PublicUserDto;
import com.campusfind.entity.MatchEntity;
import com.campusfind.entity.MatchStatus;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.ItemRepository;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemService itemService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<MatchResponseDto> getMyMatches(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Page<MatchEntity> matches = matchRepository.findUserMatchesPaginated(user.getId(), pageable);
        return matches.map(m -> toMatchResponseDto(m, user.getId()));
    }

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(Long id, String userEmail) {
        MatchEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isOwner = match.getLostItem().getOwner().getId().equals(user.getId())
                || match.getFoundItem().getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this match");
        }

        return toMatchResponseDto(match, user.getId());
    }

    @Transactional
    public MatchResponseDto rejectMatch(Long id, String userEmail) {
        MatchEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isOwner = match.getLostItem().getOwner().getId().equals(user.getId())
                || match.getFoundItem().getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to reject this match");
        }

        if (match.getStatus() == MatchStatus.RETURNED) {
            throw new ApiException("Cannot reject a match that has already been returned", HttpStatus.BAD_REQUEST);
        }

        match.setStatus(MatchStatus.REJECTED);
        MatchEntity updated = matchRepository.save(match);
        log.info("Match ID {} rejected by user {}", id, user.getEmail());

        return toMatchResponseDto(updated, user.getId());
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDto> getMatchesForItem(Long itemId, String userEmail) {
        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isOwner = item.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view matches for this item");
        }

        List<MatchEntity> matches = matchRepository.findActiveMatchesByItemId(itemId);
        return matches.stream()
                .map(m -> toMatchResponseDto(m, user.getId()))
                .collect(Collectors.toList());
    }

    public MatchResponseDto toMatchResponseDto(MatchEntity match, Long currentUserId) {
        Map<String, Object> breakdown = Collections.emptyMap();
        try {
            if (match.getScoreBreakdown() != null) {
                breakdown = objectMapper.readValue(match.getScoreBreakdown(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse score breakdown JSON: {}", e.getMessage());
        }

        // Identify who the other user is relative to the requesting user
        User otherUser;
        if (match.getLostItem().getOwner().getId().equals(currentUserId)) {
            otherUser = match.getFoundItem().getOwner();
        } else {
            otherUser = match.getLostItem().getOwner();
        }

        // Privacy rule: other user is mapped as PublicUserDto only
        PublicUserDto otherUserDto = ItemService.toPublicUserDto(otherUser);

        return MatchResponseDto.builder()
                .id(match.getId())
                .lostItem(itemService.toSummaryDto(match.getLostItem()))
                .foundItem(itemService.toSummaryDto(match.getFoundItem()))
                .score(match.getScore())
                .scoreBreakdown(breakdown)
                .status(match.getStatus())
                .otherUser(otherUserDto)
                .createdAt(match.getCreatedAt())
                .build();
    }
}
