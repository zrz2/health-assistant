package com.healthassistant.module.evaluation.dto;

import java.time.LocalDateTime;

public record EvaluationRunResponse(
        Long id,
        String runId,
        String status,
        Integer numQuestions,
        Integer completedQuestions,
        Double avgFaithfulness,
        Double avgAnswerRelevancy,
        Double avgContextPrecision,
        Double avgContextRecall,
        Double avgFactualCorrectness,
        String detailsJson,
        String errorLog,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static EvaluationRunResponse from(
            com.healthassistant.module.evaluation.entity.EvaluationRun run) {
        return new EvaluationRunResponse(
                run.getId(),
                run.getRunId(),
                run.getStatus(),
                run.getNumQuestions(),
                run.getCompletedQuestions(),
                run.getAvgFaithfulness(),
                run.getAvgAnswerRelevancy(),
                run.getAvgContextPrecision(),
                run.getAvgContextRecall(),
                run.getAvgFactualCorrectness(),
                run.getDetailsJson(),
                run.getErrorLog(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt()
        );
    }
}
