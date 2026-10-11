package com.campusfind.repository;

import com.campusfind.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Chat history by match ordered by createdAt ascending (for chronological chat scroll)
     */
    List<ChatMessage> findByMatchIdOrderByCreatedAtAsc(Long matchId);

    /**
     * Paginated chat history by match ordered by createdAt descending (for cursor pagination)
     */
    Page<ChatMessage> findByMatchIdOrderByCreatedAtDesc(Long matchId, Pageable pageable);
}
