package com.healthassistant.module.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.common.util.IdGenerator;
import com.healthassistant.common.util.RetryUtils;
import com.healthassistant.module.chat.entity.ChatMessage;
import com.healthassistant.module.chat.entity.ClarificationRecord;
import com.healthassistant.module.chat.repository.ChatMessageRepository;
import com.healthassistant.module.chat.repository.ClarificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClarificationService {

    private static final Logger log = LoggerFactory.getLogger(ClarificationService.class);

    private final ClarificationRepository clarificationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public ClarificationService(ClarificationRepository clarificationRepository,
                                ChatMessageRepository messageRepository,
                                ChatClient.Builder chatClientBuilder,
                                ObjectMapper objectMapper) {
        this.clarificationRepository = clarificationRepository;
        this.messageRepository = messageRepository;
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
    }

    public ClarificationResult checkClarification(String question, String history) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = buildClarificationPrompt(question, history);

            String response = RetryUtils.executeWithRetry(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content(),
                    "ClarificationCheck");

            return parseClarificationResponse(response);
        } catch (Exception e) {
            log.warn("Clarification check failed, skipping: {}", e.getMessage());
            return new ClarificationResult(false, "", "", "[]", "[]");
        }
    }

    @Transactional
    public ClarificationRecord createClarification(Long sessionId, String messageId,
                                                     ClarificationResult result) {
        ClarificationRecord record = new ClarificationRecord();
        record.setClarificationId(IdGenerator.generateClarificationId());
        record.setSessionId(sessionId);
        record.setOriginalMessageId(messageId);
        record.setClarificationType(result.clarificationType);
        record.setQuestion(result.question);
        record.setOptions(result.options);
        record.setMissingFields(result.missingFields);
        clarificationRepository.save(record);
        return record;
    }

    @Transactional
    public String answerClarification(String clarificationId, String answer) {
        ClarificationRecord record = clarificationRepository.findByClarificationId(clarificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLARIFICATION_NOT_FOUND, "澄清记录不存在"));

        record.setUserAnswer(answer);
        record.setStatus(1);
        record.setAnsweredAt(LocalDateTime.now());

        String originalContent = messageRepository.findByMessageId(record.getOriginalMessageId())
                .map(ChatMessage::getContent)
                .orElse("");

        String rewritten = rewriteQuery(originalContent, record.getQuestion(), answer);
        record.setRewrittenQuery(rewritten);
        clarificationRepository.save(record);

        return rewritten;
    }

    private String rewriteQuery(String original, String clarificationQuestion, String userAnswer) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = String.format(
                    "用户原始问题：%s\n系统追问：%s\n用户回答：%s\n请将上述对话整合为一个完整的医疗咨询问题，直接输出改写后的问题。",
                    original, clarificationQuestion, userAnswer);

            return RetryUtils.executeWithRetry(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content(),
                    "ClarifyRewrite");
        } catch (Exception e) {
            log.warn("Clarification rewrite failed, using original: {}", e.getMessage());
            return original;
        }
    }

    private String buildClarificationPrompt(String question, String history) {
        // Simple inline prompt for clarification detection
        return String.format("""
                你是一个医疗问诊意图分析器。分析用户的问题，判断是否需要进一步澄清才能给出准确回答。

                ## 需要澄清的情况
                1. 症状描述模糊，缺少具体部位、持续时间、程度
                2. 问题缺少关键上下文
                3. 意图不明确

                ## 对话历史
                %s

                ## 用户问题
                %s

                ## 输出格式（严格JSON）
                {"needsClarification": true/false, "clarificationType": "...", "question": "...", "options": [...], "missingFields": [...]}
                """, history, question);
    }

    private ClarificationResult parseClarificationResponse(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);
            return new ClarificationResult(
                    node.path("needsClarification").asBoolean(false),
                    node.path("clarificationType").asText(""),
                    node.path("question").asText(""),
                    node.path("options").toString(),
                    node.path("missingFields").toString()
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse clarification response: {}", response);
            return new ClarificationResult(false, "", "", "[]", "[]");
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

    public record ClarificationResult(boolean needsClarification, String clarificationType,
                                       String question, String options, String missingFields) {}
}
