package com.healthassistant.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "knowledge_source")
public class KnowledgeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String url;

    @Column(length = 50)
    private String sourceType; // clinical_guideline, medical_textbook, research_paper, drug_manual, health_encyclopedia

    @Column(nullable = false)
    private Integer defaultEvidenceLevel = 3;

    @Column(length = 50)
    private String parserType;

    @Column(columnDefinition = "TEXT")
    private String configJson;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
