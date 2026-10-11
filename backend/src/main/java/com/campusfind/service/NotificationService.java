package com.campusfind.service;

import com.campusfind.dto.response.NotificationDto;
import com.campusfind.entity.Notification;
import com.campusfind.entity.User;
import com.campusfind.exception.ApiException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.NotificationRepository;
import com.campusfind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(User recipient, String title, String body, String link) {
        if (recipient == null) {
            log.warn("Cannot create notification for null recipient");
            return null;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .title(title)
                .body(body)
                .link(link)
                .read(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification createNotification(Long userId, String title, String body, String link) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return createNotification(recipient, title, body, link);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationDto markAsRead(String userEmail, Long notificationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ApiException("Access denied: You can only mark your own notifications as read", HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return toDto(saved);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(user.getId());
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public boolean hasRecentNotificationForLink(Long userId, String link, int withinMinutes) {
        Optional<Notification> recent = notificationRepository.findTopByUserIdAndLinkOrderByCreatedAtDesc(userId, link);
        if (recent.isEmpty()) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(withinMinutes);
        return recent.get().getCreatedAt().isAfter(cutoff);
    }

    public NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .link(notification.getLink())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
