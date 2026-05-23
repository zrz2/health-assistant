package com.healthassistant.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sync_task")
public class SyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String taskId;

    @Column(nullable = false, length = 100)
    private String sourceName;

    @Column(length = 50)
    private String syncType; // full, incremental

    @Column(nullable = false)
    private Integer status = 0; // 0-待执行 1-执行中 2-成功 3-失败

    private Integer totalItems;

    private Integer successItems;

    private Integer failedItems;

    @Column(columnDefinition = "TEXT")
    private String errorLog;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
