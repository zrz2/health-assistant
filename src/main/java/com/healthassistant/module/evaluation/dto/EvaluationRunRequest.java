package com.healthassistant.module.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EvaluationRunRequest(
        @Min(10) @Max(500) int numQuestions
) {}
