package com.campusfind.service;

import com.campusfind.dto.request.CreateItemRequest;
import com.campusfind.dto.request.UpdateItemRequest;
import com.campusfind.dto.response.ItemDetailDto;
import com.campusfind.dto.response.ItemSummaryDto;
import com.campusfind.dto.response.PublicUserDto;
import com.campusfind.entity.*;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.*;
import com.campusfind.service.matching.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final RoomRepository roomRepository;
    private final ReportRepository reportRepository;
    private final MatchingService matchingService;

    @Transactional
    public ItemDetailDto createItem(String userEmail, CreateItemRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (user.isBanned()) {
            throw new ApiException("Banned users cannot create posts", HttpStatus.FORBIDDEN);
        }

        // Daily rate limit: max 10 posts per user per 24 hours
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentPostCount = itemRepository.countByOwnerIdAndCreatedAtAfter(user.getId(), twentyFourHoursAgo);
        if (recentPostCount >= 10) {
            throw new ApiException("Daily post limit reached: Maximum 10 items per 24 hours permitted", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Event time validation: cannot be in the future, cannot be older than 30 days
        LocalDateTime now = LocalDateTime.now();
        if (request.getEventTime().isAfter(now)) {
            throw new ApiException("Event time cannot be in the future", HttpStatus.BAD_REQUEST);
        }
        if (request.getEventTime().isBefore(now.minusDays(30))) {
            throw new ApiException("Event time cannot be older than 30 days", HttpStatus.BAD_REQUEST);
        }

        // FOUND item verification rules
        boolean handedToOffice = false;
        String verificationQuestion = null;

        if (request.getType() == ItemType.FOUND) {
            handedToOffice = request.isHandedToOffice();
            verificationQuestion = request.getVerificationQuestion();

            if (!handedToOffice && (verificationQuestion == null || verificationQuestion.trim().isEmpty())) {
                throw new ApiException("Verification question is required for found items unless handed to security/office", HttpStatus.BAD_REQUEST);
            }
        }

        // Location resolution
        Block block = null;
        if (request.getBlockId() != null) {
            block = blockRepository.findById(request.getBlockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Block", "id", request.getBlockId()));
        }

        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));
            if (block != null && !room.getBlock().getId().equals(block.getId())) {
                throw new ApiException("Selected room does not belong to the selected block", HttpStatus.BAD_REQUEST);
            }
            if (block == null) {
                block = room.getBlock();
            }
        }

        Item item = Item.builder()
                .owner(user)
                .type(request.getType())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .block(block)
                .room(room)
                .eventTime(request.getEventTime())
                .imageUrl(request.getImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .handedToOffice(handedToOffice)
                .verificationQuestion(verificationQuestion)
                .status(ItemStatus.OPEN)
                .hidden(false)
                .build();

        Item savedItem = itemRepository.save(item);

        // Run matching engine asynchronously
        matchingService.runMatchingAsync(savedItem.getId());

        return toDetailDto(savedItem);
    }

    @Transactional(readOnly = true)
    public Page<ItemSummaryDto> getItems(
            String userEmail,
            ItemType type,
            ItemCategory category,
            Long blockId,
            Long roomId,
            ItemStatus status,
            String keyword,
            Boolean mine,
            Pageable pageable
    ) {
        User user = (userEmail != null) ? userRepository.findByEmail(userEmail).orElse(null) : null;
        Long ownerId = (Boolean.TRUE.equals(mine) && user != null) ? user.getId() : null;
        boolean includeHidden = (user != null && user.getRole() == Role.ADMIN);

        Page<Item> itemsPage = itemRepository.findItemsWithFilters(
                type, category, blockId, roomId, status, keyword, ownerId, includeHidden, pageable
        );

        return itemsPage.map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public ItemDetailDto getItemById(Long id, String userEmail) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        User user = (userEmail != null) ? userRepository.findByEmail(userEmail).orElse(null) : null;
        boolean isOwnerOrAdmin = (user != null && (user.getId().equals(item.getOwner().getId()) || user.getRole() == Role.ADMIN));

        if (item.isHidden() && !isOwnerOrAdmin) {
            throw new ResourceNotFoundException("Item", "id", id);
        }

        return toDetailDto(item);
    }

    @Transactional
    public ItemDetailDto updateItem(Long id, String userEmail, UpdateItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isOwner = item.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to modify this item");
        }

        if (item.getStatus() == ItemStatus.RETURNED) {
            throw new ApiException("Items that are already marked RETURNED cannot be modified", HttpStatus.BAD_REQUEST);
        }

        if (request.getTitle() != null) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }
        if (request.getLatitude() != null) {
            item.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            item.setLongitude(request.getLongitude());
        }
        if (request.getHandedToOffice() != null && item.getType() == ItemType.FOUND) {
            item.setHandedToOffice(request.getHandedToOffice());
        }
        if (request.getVerificationQuestion() != null && item.getType() == ItemType.FOUND) {
            item.setVerificationQuestion(request.getVerificationQuestion());
        }

        if (request.getBlockId() != null) {
            Block block = blockRepository.findById(request.getBlockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Block", "id", request.getBlockId()));
            item.setBlock(block);
        }

        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));
            item.setRoom(room);
            if (item.getBlock() == null) {
                item.setBlock(room.getBlock());
            }
        }

        Item saved = itemRepository.save(item);
        return toDetailDto(saved);
    }

    @Transactional
    public void deleteItem(Long id, String userEmail) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isOwner = item.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to delete this item");
        }

        itemRepository.delete(item);
    }

    @Transactional
    public void reportItem(Long itemId, String userEmail, String reason) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (item.getOwner().getId().equals(user.getId())) {
            throw new ApiException("You cannot report your own item", HttpStatus.BAD_REQUEST);
        }

        if (reportRepository.existsByItemIdAndReporterId(itemId, user.getId())) {
            throw new ApiException("You have already reported this item", HttpStatus.BAD_REQUEST);
        }

        Report report = Report.builder()
                .item(item)
                .reporter(user)
                .reason(reason.trim())
                .build();
        reportRepository.save(report);

        long reportCount = reportRepository.countByItemId(itemId);
        if (reportCount >= 3) {
            item.setHidden(true);
            itemRepository.save(item);
            log.warn("Item ID {} has received {} reports and has been automatically hidden.", itemId, reportCount);
        }
    }

    public ItemSummaryDto toSummaryDto(Item item) {
        return ItemSummaryDto.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .description(item.getDescription())
                .category(item.getCategory())
                .blockName(item.getBlock() != null ? item.getBlock().getName() : null)
                .roomName(item.getRoom() != null ? item.getRoom().getName() : null)
                .eventTime(item.getEventTime())
                .imageUrl(item.getImageUrl())
                .status(item.getStatus())
                .handedToOffice(item.isHandedToOffice())
                .owner(toPublicUserDto(item.getOwner()))
                .createdAt(item.getCreatedAt())
                .build();
    }

    public ItemDetailDto toDetailDto(Item item) {
        return ItemDetailDto.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .description(item.getDescription())
                .category(item.getCategory())
                .blockId(item.getBlock() != null ? item.getBlock().getId() : null)
                .blockName(item.getBlock() != null ? item.getBlock().getName() : null)
                .roomId(item.getRoom() != null ? item.getRoom().getId() : null)
                .roomName(item.getRoom() != null ? item.getRoom().getName() : null)
                .eventTime(item.getEventTime())
                .imageUrl(item.getImageUrl())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .handedToOffice(item.isHandedToOffice())
                .verificationQuestion(item.getVerificationQuestion())
                .status(item.getStatus())
                .hidden(item.isHidden())
                .owner(toPublicUserDto(item.getOwner()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public static PublicUserDto toPublicUserDto(User user) {
        return PublicUserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .year(user.getYear())
                .avatarUrl(user.getAvatarUrl())
                .karmaPoints(user.getKarmaPoints())
                .build();
    }
}
