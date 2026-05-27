package com.healthassistant.module.evaluation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "evaluation_run", indexes = {
        @Index(name = "idx_eval_run_id", columnList = "runId", unique = true)
})
public class EvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String runId;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(nullable = false)
    private Integer numQuestions;

    private Integer completedQuestions;

    private Double avgFaithfulness;

    private Double avgAnswerRelevancy;

    private Double avgContextPrecision;

    private Double avgContextRecall;

    private Double avgFactualCorrectness;

    @Column(columnDefinition = "LONGTEXT")
    private String detailsJson;

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
