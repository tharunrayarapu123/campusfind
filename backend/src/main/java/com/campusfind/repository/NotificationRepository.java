package com.campusfind.repository;

import com.campusfind.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Unread notification count for a user
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Notifications by user ordered by createdAt descending
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndReadFalse(Long userId);

    Optional<Notification> findTopByUserIdAndLinkOrderByCreatedAtDesc(Long userId, String link);
}
