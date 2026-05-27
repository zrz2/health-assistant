package com.healthassistant.module.evaluation.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluationResultSubmitRequest(
        @NotBlank String runId,
        Double avgFaithfulness,
        Double avgAnswerRelevancy,
        Double avgContextPrecision,
        Double avgContextRecall,
        Double avgFactualCorrectness,
        Object details
) {}
