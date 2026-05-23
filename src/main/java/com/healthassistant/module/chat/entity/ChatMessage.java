package com.healthassistant.module.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(name = "session_ref_id", nullable = false)
    private Long sessionId;

    @Column(length = 64)
    private String parentMessageId;

    @Column(nullable = false)
    private Integer messageType; // 1-user 2-assistant 3-system 4-clarification

    @Column(length = 20)
    private String contentType = "text";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String contentHtml;

    private Integer evidenceLevel;

    @Column(columnDefinition = "JSON")
    private String sources;

    @Column(columnDefinition = "JSON")
    private String clarificationData;

    private Integer feedbackType; // 1-thumbs up 0-thumbs down

    @Column(columnDefinition = "TEXT")
    private String feedbackComment;

    private Integer tokensUsed;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
