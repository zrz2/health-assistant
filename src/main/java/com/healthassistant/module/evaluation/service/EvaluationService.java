package com.healthassistant.module.evaluation.service;

import com.healthassistant.module.evaluation.dto.*;
import com.healthassistant.module.evaluation.entity.EvaluationRun;
import com.healthassistant.module.evaluation.repository.EvaluationRunRepository;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import com.healthassistant.module.rag.service.ContextBuilder;
import com.healthassistant.module.rag.service.RagService;
import com.healthassistant.module.rag.service.RetrieverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationRunRepository runRepository;
    private final RetrieverService retrieverService;
    private final ContextBuilder contextBuilder;
    private final RagService ragService;
    private final ChatClient.Builder chatClientBuilder;
    private final EvaluationScriptRunner scriptRunner;

    @Value("${app.rag.top-k:5}")
    private int topK;

    public EvaluationService(EvaluationRunRepository runRepository,
                             RetrieverService retrieverService,
                             ContextBuilder contextBuilder,
                             RagService ragService,
                             ChatClient.Builder chatClientBuilder,
                             EvaluationScriptRunner scriptRunner) {
        this.runRepository = runRepository;
        this.retrieverService = retrieverService;
        this.contextBuilder = contextBuilder;
        this.ragService = ragService;
        this.chatClientBuilder = chatClientBuilder;
        this.scriptRunner = scriptRunner;
    }

    @Transactional
    public EvaluationRun triggerEvaluation(int numQuestions) {
        String runId = "eval-" + UUID.randomUUID().toString().substring(0, 8);

        EvaluationRun run = new EvaluationRun();
        run.setRunId(runId);
        run.setStatus("PENDING");
        run.setNumQuestions(numQuestions);
        run = runRepository.saveAndFlush(run);

        log.info("Evaluation run {} created with {} questions", runId, numQuestions);

        // Launch Python evaluation script asynchronously
        scriptRunner.launch(runId, numQuestions);

        return run;
    }

    /**
     * Synchronous RAG answer generation for evaluation.
     * Bypasses ChatService entirely — no clarification, no sensitive-word filter, no SSE.
     */
    public EvaluationSyncChatResponse generateAnswerWithContext(String question) {
        List<RetrievedDocument> documents;
        try {
            RetrieverService.RetrieveResult result = retrieverService.retrieve(question, null, topK);
            documents = result.documents();
        } catch (Exception e) {
            log.warn("Retrieval failed for evaluation question, using empty context: {}", e.getMessage());
            documents = List.of();
        }

        String context = documents.isEmpty()
                ? "（暂无相关医学知识参考）"
                : contextBuilder.buildWithParentContext(documents);

        String prompt = ragService.buildRagPrompt(question, "（评测模式）", context);

        List<String> contexts = documents.stream()
                .map(RetrievedDocument::getContent)
                .collect(Collectors.toList());

        String answer;
        try {
            ChatClient chatClient = chatClientBuilder.build();
            answer = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("LLM call failed for evaluation question", e);
            answer = "[生成失败: " + e.getMessage() + "]";
        }

        return new EvaluationSyncChatResponse(answer, contexts);
    }

    @Transactional
    public void submitResults(EvaluationResultSubmitRequest request) {
        EvaluationRun run = runRepository.findByRunId(request.runId())
                .orElseThrow(() -> new RuntimeException("Evaluation run not found: " + request.runId()));

        run.setAvgFaithfulness(request.avgFaithfulness());
        run.setAvgAnswerRelevancy(request.avgAnswerRelevancy());
        run.setAvgContextPrecision(request.avgContextPrecision());
        run.setAvgContextRecall(request.avgContextRecall());
        run.setAvgFactualCorrectness(request.avgFactualCorrectness());
        run.setCompletedQuestions(run.getNumQuestions()); // assume all completed
        run.setStatus("COMPLETED");
        run.setCompletedAt(LocalDateTime.now());

        if (request.details() != null) {
            try {
                run.setDetailsJson(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(request.details()));
            } catch (Exception e) {
                log.warn("Failed to serialize evaluation details", e);
            }
        }

        runRepository.save(run);
        log.info("Evaluation run {} completed: faithfulness={}, relevancy={}, precision={}, recall={}",
                request.runId(), request.avgFaithfulness(), request.avgAnswerRelevancy(),
                request.avgContextPrecision(), request.avgContextRecall());
    }

    @Transactional(readOnly = true)
    public Page<EvaluationRunResponse> listRuns(Pageable pageable) {
        return runRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(EvaluationRunResponse::from);
    }

    @Transactional(readOnly = true)
    public EvaluationRunResponse getRun(String runId) {
        EvaluationRun run = runRepository.findByRunId(runId)
                .orElseThrow(() -> new RuntimeException("Evaluation run not found: " + runId));
        return EvaluationRunResponse.from(run);
    }
}
