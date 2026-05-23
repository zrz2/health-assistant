package com.healthassistant.module.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "clarification_record")
public class ClarificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String clarificationId;

    @Column(name = "session_ref_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 64)
    private String originalMessageId;

    @Column(nullable = false, length = 50)
    private String clarificationType; // missing_context, ambiguity, vague_intent

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "JSON")
    private String options; // JSON array of choices

    @Column(columnDefinition = "JSON")
    private String missingFields; // JSON array of missing field names

    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    @Column(columnDefinition = "TEXT")
    private String rewrittenQuery;

    @Column(nullable = false)
    private Integer status = 0; // 0-待回答 1-已回答

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
