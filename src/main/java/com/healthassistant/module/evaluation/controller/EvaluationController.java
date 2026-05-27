package com.healthassistant.module.evaluation.controller;

import com.healthassistant.common.result.Result;
import com.healthassistant.module.evaluation.dto.*;
import com.healthassistant.module.evaluation.entity.EvaluationRun;
import com.healthassistant.module.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/evaluation")
@PreAuthorize("hasRole('ADMIN')")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    public Result<Map<String, String>> triggerEvaluation(
            @Valid @RequestBody EvaluationRunRequest request) {
        EvaluationRun run = evaluationService.triggerEvaluation(request.numQuestions());
        return Result.success(Map.of(
                "runId", run.getRunId(),
                "status", run.getStatus()
        ));
    }

    @PostMapping("/generate")
    public Result<EvaluationSyncChatResponse> generateForEvaluation(
            @Valid @RequestBody EvaluationSyncChatRequest request) {
        return Result.success(evaluationService.generateAnswerWithContext(request.question()));
    }

    @PostMapping("/runs/{runId}/results")
    public Result<Void> submitResults(
            @PathVariable String runId,
            @Valid @RequestBody EvaluationResultSubmitRequest request) {
        evaluationService.submitResults(request);
        return Result.success();
    }

    @GetMapping("/runs")
    public Result<Page<EvaluationRunResponse>> listRuns(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return Result.success(evaluationService.listRuns(pageable));
    }

    @GetMapping("/runs/{runId}")
    public Result<EvaluationRunResponse> getRun(@PathVariable String runId) {
        return Result.success(evaluationService.getRun(runId));
    }
}
