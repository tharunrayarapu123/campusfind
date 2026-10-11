package com.campusfind.service;

import com.campusfind.dto.request.ChangePasswordRequest;
import com.campusfind.dto.request.UpdateUserRequest;
import com.campusfind.dto.response.PublicUserDto;
import com.campusfind.dto.response.UserResponse;
import com.campusfind.entity.User;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.campusfind.repository.ItemRepository itemRepository;
    private final com.campusfind.repository.MatchRepository matchRepository;

    @Transactional
    public UserResponse updateProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setFullName(request.getFullName().trim());
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment().trim());
        }
        if (request.getYear() != null) {
            user.setYear(request.getYear());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updatedUser = userRepository.save(user);
        long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(updatedUser.getId(), com.campusfind.entity.MatchStatus.RETURNED);
        return AuthService.toUserResponse(updatedUser, returnsCount);
    }

    @Transactional
    public UserResponse updateSettings(String email, com.campusfind.dto.request.UpdateSettingsRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }

        User updatedUser = userRepository.save(user);
        long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(updatedUser.getId(), com.campusfind.entity.MatchStatus.RETURNED);
        return AuthService.toUserResponse(updatedUser, returnsCount);
    }

    @Transactional(readOnly = true)
    public com.campusfind.dto.response.UserStatsDto getUserStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        long postsCount = itemRepository.countByOwnerId(user.getId());
        long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(user.getId(), com.campusfind.entity.MatchStatus.RETURNED);
        java.util.List<String> badges = com.campusfind.util.BadgeUtil.computeBadges(returnsCount);

        return com.campusfind.dto.response.UserStatsDto.builder()
                .postsCount(postsCount)
                .returnsCount(returnsCount)
                .karmaPoints(user.getKarmaPoints())
                .badges(badges)
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.List<com.campusfind.dto.response.LeaderboardUserDto> getLeaderboard() {
        java.util.List<User> topUsers = userRepository.findTop20ByBannedFalseOrderByKarmaPointsDesc();
        return topUsers.stream()
                .map(u -> {
                    long returnsCount = matchRepository.countByFoundItemOwnerIdAndStatus(u.getId(), com.campusfind.entity.MatchStatus.RETURNED);
                    return com.campusfind.dto.response.LeaderboardUserDto.builder()
                            .id(u.getId())
                            .fullName(u.getFullName())
                            .department(u.getDepartment())
                            .year(u.getYear())
                            .avatarUrl(u.getAvatarUrl())
                            .karmaPoints(u.getKarmaPoints())
                            .returnsCount(returnsCount)
                            .badges(com.campusfind.util.BadgeUtil.computeBadges(returnsCount))
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException("Current password does not match", HttpStatus.BAD_REQUEST);
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ApiException("New password cannot be the same as current password", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PublicUserDto getPublicUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Privacy rule: phone and email are never exposed in PublicUserDto
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
