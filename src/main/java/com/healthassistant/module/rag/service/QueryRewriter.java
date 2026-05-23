package com.healthassistant.module.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthassistant.common.util.RetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriter.class);

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public QueryRewriter(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
    }

    public RewriteResult rewrite(String question, String history) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = buildRewritePrompt(question, history);

            String response = RetryUtils.executeWithRetry(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content(),
                    "QueryRewrite");

            return parseRewriteResponse(response, question);
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original query: {}", e.getMessage());
            return new RewriteResult(question, List.of(question), List.of(), "general");
        }
    }

    private String buildRewritePrompt(String question, String history) {
        return String.format("""
                你是一个医疗查询改写器。将用户的口语化健康问题改写为更精确的检索查询。

                ## 改写规则
                1. 将口语表达转为专业医学术语（如"血糖高"→"高血糖"）
                2. 补全医学术语的全称（如"二甲"→"二甲双胍"）
                3. 提取核心医疗概念，生成2-3个检索变体
                4. 保留用户原始意图和限制条件

                ## 对话历史
                %s

                ## 用户问题
                %s

                ## 输出格式（严格JSON）
                {"rewrittenQuery": "...", "searchQueries": ["q1", "q2", "q3"], "extractedEntities": ["e1", "e2"], "intentType": "..."}
                """, history.isEmpty() ? "无" : history, question);
    }

    private RewriteResult parseRewriteResponse(String response, String original) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);

            List<String> searchQueries = new ArrayList<>();
            node.path("searchQueries").forEach(q -> searchQueries.add(q.asText()));
            if (searchQueries.isEmpty()) {
                searchQueries.add(node.path("rewrittenQuery").asText(original));
            }

            List<String> entities = new ArrayList<>();
            node.path("extractedEntities").forEach(e -> entities.add(e.asText()));

            return new RewriteResult(
                    node.path("rewrittenQuery").asText(original),
                    searchQueries,
                    entities,
                    node.path("intentType").asText("general")
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse rewrite response, using original query");
            return new RewriteResult(original, List.of(original), List.of(), "general");
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "{}";
    }

    public record RewriteResult(String rewrittenQuery, List<String> searchQueries,
                                 List<String> extractedEntities, String intentType) {}
}
