package com.healthassistant.module.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String operation;

    @Column(length = 100)
    private String method;

    @Column(columnDefinition = "TEXT")
    private String params;

    @Column(length = 50)
    private String ip;

    @Column(nullable = false)
    private Long executionTime;

    @Column(nullable = false, length = 10)
    private String result;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
