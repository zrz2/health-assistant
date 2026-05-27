package com.healthassistant.module.evaluation.dto;

import java.util.List;

public record EvaluationSyncChatResponse(
        String answer,
        List<String> contexts
) {}
