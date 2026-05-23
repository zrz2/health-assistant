package com.healthassistant.module.chat.repository;

import com.healthassistant.module.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByMessageId(String messageId);

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    Page<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);

    List<ChatMessage> findBySessionIdAndMessageTypeOrderByCreatedAtAsc(Long sessionId, Integer messageType);

    int countBySessionId(Long sessionId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByFeedbackType(Integer feedbackType);
}
