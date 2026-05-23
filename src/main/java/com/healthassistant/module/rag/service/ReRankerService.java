package com.healthassistant.module.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthassistant.common.util.RetryUtils;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReRankerService {

    private static final Logger log = LoggerFactory.getLogger(ReRankerService.class);

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public ReRankerService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Cross-encoder style re-ranking using LLM batch scoring.
     * In production, replace with bge-reranker-v2-m3 deployed separately.
     */
    public List<RetrievedDocument> rerank(String query, List<RetrievedDocument> documents, int topK) {
        if (documents.size() <= topK) {
            return documents;
        }

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = buildBatchScorePrompt(query, documents);
            String response = RetryUtils.executeWithRetry(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content(),
                    "BatchReRank");

            double[] scores = parseScores(response, documents.size());
            if (scores == null) {
                log.warn("Failed to parse batch re-rank scores, returning first {} docs", topK);
                return documents.subList(0, Math.min(topK, documents.size()));
            }

        List<ScoredDoc> scored = new ArrayList<>();
        for (int i = 0; i < Math.min(documents.size(), scores.length); i++) {
            scored.add(new ScoredDoc(documents.get(i), scores[i]));
        }

        List<RetrievedDocument> reranked = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
                .limit(topK)
                .peek(s -> s.doc.setScore(s.score))
                .map(ScoredDoc::doc)
                .collect(Collectors.toList());

        log.debug("Re-ranked {} -> {} documents", documents.size(), reranked.size());
        return reranked;
        } catch (Exception e) {
            log.warn("Re-rank LLM call failed, returning top {} docs without re-ranking: {}", topK, e.getMessage());
            return documents.subList(0, Math.min(topK, documents.size()));
        }
    }

    private String buildBatchScorePrompt(String query, List<RetrievedDocument> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("评估以下文档与查询的相关性，为每个文档输出0-1之间的分数（1=高度相关，0=无关）。\n\n");
        sb.append("查询: ").append(query).append("\n\n");

        for (int i = 0; i < documents.size(); i++) {
            String content = documents.get(i).getContent();
            String snippet = content.length() > 300 ? content.substring(0, 300) : content;
            sb.append("文档").append(i).append(": ").append(snippet).append("\n");
        }

        sb.append("\n输出JSON数组，按文档顺序给出分数，如: [0.9, 0.5, 0.2, ...]\n");
        sb.append("分数数组:");
        return sb.toString();
    }

    private double[] parseScores(String response, int expectedSize) {
        try {
            String json = extractJsonArray(response);
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return null;
            double[] scores = new double[node.size()];
            for (int i = 0; i < node.size(); i++) {
                scores[i] = node.get(i).asDouble(0.5);
            }
            return scores;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "[]";
    }

    private record ScoredDoc(RetrievedDocument doc, double score) {}
}
