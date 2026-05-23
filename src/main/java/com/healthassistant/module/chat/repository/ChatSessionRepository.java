package com.healthassistant.module.chat.repository;

import com.healthassistant.module.chat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    Page<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, Integer status, Pageable pageable);

    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    List<ChatSession> findByUserIdAndStatus(Long userId, Integer status);

    long countByUserId(Long userId);

    long countByUpdatedAtAfter(LocalDateTime after);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
