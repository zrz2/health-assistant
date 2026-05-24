package com.healthassistant.module.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_session", indexes = {
    @Index(name = "idx_session_user_status", columnList = "userId, status, updatedAt"),
    @Index(name = "idx_session_updated", columnList = "updatedAt")
})
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionId;

    private Long userId; // null for guest

    @Column(length = 200)
    private String title;

    @Column(nullable = false)
    private Integer status = 1; // 0-结束 1-进行中

    @Column(nullable = false)
    private Integer messageCount = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
