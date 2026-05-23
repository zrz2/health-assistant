package com.healthassistant.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "knowledge_item", indexes = {
        @Index(name = "idx_doc_id", columnList = "docId", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
public class KnowledgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String docId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 100)
    private String documentType;

    @Column(length = 200)
    private String sourceName;

    @Column(length = 500)
    private String sourceUrl;

    private Integer evidenceLevel;

    private LocalDate publicationDate;

    @Column(nullable = false)
    private Integer status = 0; // 0-待处理 1-已切分 2-已向量化 3-已索引

    private Integer chunkCount;

    @Column(updatable = false)
    private LocalDateTime createdAt;

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
