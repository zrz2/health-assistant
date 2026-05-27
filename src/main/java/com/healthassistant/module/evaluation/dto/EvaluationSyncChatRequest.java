package com.healthassistant.module.evaluation.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluationSyncChatRequest(
        @NotBlank String question
) {}
