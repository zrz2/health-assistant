package com.healthassistant.module.chat.repository;

import com.healthassistant.module.chat.entity.ClarificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClarificationRepository extends JpaRepository<ClarificationRecord, Long> {

    Optional<ClarificationRecord> findByClarificationId(String clarificationId);

    Optional<ClarificationRecord> findByOriginalMessageId(String originalMessageId);

    List<ClarificationRecord> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
