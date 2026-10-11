package com.campusfind.service;

import com.campusfind.dto.response.AdminStatsDto;
import com.campusfind.dto.response.ClaimResponse;
import com.campusfind.dto.response.ReportDto;
import com.campusfind.dto.response.UserResponse;
import com.campusfind.entity.Item;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.ItemType;
import com.campusfind.entity.MatchStatus;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.ItemRepository;
import com.campusfind.repository.MatchRepository;
import com.campusfind.repository.ReportRepository;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final MatchRepository matchRepository;
    private final ReportRepository reportRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ClaimService claimService;

    @Transactional(readOnly = true)
    public AdminStatsDto getAdminStats() {
        long totalUsers = userRepository.count();
        long totalLost = itemRepository.countByType(ItemType.LOST);
        long totalFound = itemRepository.countByType(ItemType.FOUND);
        long returned = itemRepository.countByStatus(ItemStatus.RETURNED);
        long openItems = itemRepository.countByStatus(ItemStatus.OPEN);
        long matchesCount = matchRepository.count();

        long totalItems = totalLost + totalFound;
        double returnRate = totalItems > 0 ? ((double) returned / totalItems) * 100.0 : 0.0;
        returnRate = Math.round(returnRate * 100.0) / 100.0;

        Map<String, Long> itemsPerBlock = new HashMap<>();
        List<Object[]> blockCounts = itemRepository.countItemsPerBlock();
        for (Object[] row : blockCounts) {
            String blockName = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            itemsPerBlock.put(blockName, count);
        }

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .totalLost(totalLost)
                .totalFound(totalFound)
                .returned(returned)
                .returnRatePercentage(returnRate)
                .openItems(openItems)
                .matchesCount(matchesCount)
                .itemsPerBlock(itemsPerBlock)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ReportDto> getReports(Pageable pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(r -> ReportDto.builder()
                        .id(r.getId())
                        .itemId(r.getItem().getId())
                        .itemTitle(r.getItem().getTitle())
                        .reporter(ItemService.toPublicUserDto(r.getReporter()))
                        .reason(r.getReason())
                        .createdAt(r.getCreatedAt())
                        .build());
    }

    @Transactional
    public void dismissReports(Long itemId, String adminEmail) {
        User admin = getAdminOrThrow(adminEmail);
        reportRepository.deleteByItemId(itemId);
        auditLogService.logAction(admin, "DISMISS_REPORTS", "ITEM", itemId, "Dismissed reports for item ID " + itemId);
    }

    @Transactional
    public void deleteItem(Long itemId, String adminEmail) {
        User admin = getAdminOrThrow(adminEmail);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        User owner = item.getOwner();
        String itemTitle = item.getTitle();

        itemRepository.delete(item);

        notificationService.createNotification(
                owner,
                "Item Removed by Admin",
                "Your post '" + itemTitle + "' was removed by campus administration due to community policies.",
                null
        );

        auditLogService.logAction(admin, "DELETE_ITEM", "ITEM", itemId, "Deleted item post: " + itemTitle);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findAll(pageable)
                    .map(u -> {
                        long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(u.getId(), MatchStatus.RETURNED);
                        return AuthService.toUserResponse(u, returnsCount);
                    });
        }

        return userRepository.searchUsers(search.trim(), pageable)
                .map(u -> {
                    long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(u.getId(), MatchStatus.RETURNED);
                    return AuthService.toUserResponse(u, returnsCount);
                });
    }

    @Transactional
    public UserResponse banUser(Long userId, String adminEmail) {
        User admin = getAdminOrThrow(adminEmail);
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (targetUser.getId().equals(admin.getId())) {
            throw new ApiException("Administrators cannot ban their own account", HttpStatus.BAD_REQUEST);
        }

        if (targetUser.getRole() == Role.ADMIN) {
            throw new ApiException("Administrators cannot ban another administrator", HttpStatus.BAD_REQUEST);
        }

        targetUser.setBanned(true);
        User saved = userRepository.save(targetUser);

        auditLogService.logAction(admin, "BAN_USER", "USER", userId, "Banned student user " + targetUser.getEmail());

        long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(saved.getId(), MatchStatus.RETURNED);
        return AuthService.toUserResponse(saved, returnsCount);
    }

    @Transactional
    public UserResponse unbanUser(Long userId, String adminEmail) {
        User admin = getAdminOrThrow(adminEmail);
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        targetUser.setBanned(false);
        User saved = userRepository.save(targetUser);

        auditLogService.logAction(admin, "UNBAN_USER", "USER", userId, "Unbanned student user " + targetUser.getEmail());

        long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(saved.getId(), MatchStatus.RETURNED);
        return AuthService.toUserResponse(saved, returnsCount);
    }

    @Transactional
    public void unhideItem(Long itemId, String adminEmail) {
        User admin = getAdminOrThrow(adminEmail);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        item.setHidden(false);
        itemRepository.save(item);

        auditLogService.logAction(admin, "UNHIDE_ITEM", "ITEM", itemId, "Unhid auto-flagged item: " + item.getTitle());
    }

    @Transactional
    public ClaimResponse officeApproveClaim(Long matchId, Long claimId, String adminEmail) {
        return claimService.approveOfficeClaim(matchId, claimId, adminEmail);
    }

    private User getAdminOrThrow(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (user.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied: Administrative privileges required", HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
