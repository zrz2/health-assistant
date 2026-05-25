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

    public ClarificationResult checkClarification(String question, String history, String healthProfile) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = buildClarificationPrompt(question, history, healthProfile);

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
            String prompt = String.format("""
                    将下面的对话整合为一个完整的医疗咨询问题，用于直接向医疗助手提问。

                    用户原始问题：%s
                    系统追问：%s
                    用户补充信息：%s

                    要求：
                    1. 以用户第一人称重写，将补充信息融入问题中
                    2. 保持原问题的咨询意图不变
                    3. 只输出改写后的问题本身，不要添加"改写后的问题："等任何前缀或解释
                    4. 问题应完整、具体、可直接回答
                    """,
                    original, clarificationQuestion, userAnswer);

            String result = RetryUtils.executeWithRetry(() ->
                    chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content(),
                    "ClarifyRewrite");

            // Clean up common LLM artifacts
            result = result.trim();
            result = result.replaceAll("^改写后的问题[：:]\\s*", "");
            result = result.replaceAll("^问题[：:]\\s*", "");
            result = result.replaceAll("^整合后的问题[：:]\\s*", "");
            result = result.replaceAll("^"+original+"\\s*", ""); // remove if LLM just repeated original
            return result.isBlank() ? original : result;
        } catch (Exception e) {
            log.warn("Clarification rewrite failed, using original: {}", e.getMessage());
            return original;
        }
    }

    private String buildClarificationPrompt(String question, String history, String healthProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是一个医疗问诊意图分析器。分析用户的问题，判断是否需要进一步澄清才能给出准确回答。

                ## 需要澄清的情况（仅在以下情况触发）
                1. 症状描述过于模糊，完全无法给出任何有用建议（如仅说"不舒服"、"难受"）
                2. 问题缺少关键信息，且该信息直接影响回答方向

                ## 不需要澄清的情况
                1. 问题已经具体明确，包含症状、部位、持续时间等足够信息
                2. 只是轻微缺少细节，但仍能给出有意义的通用建议
                3. 用户描述的是通用健康咨询（如"高血压注意什么"）
                4. 缺少的信息对回答影响不大（如血型、身高对感冒问题）
                5. **重要**：如果用户询问自己的健康档案信息（如"我有什么慢性病"、"我对什么过敏"、"我吃什么药"），且下方已提供用户健康档案，则不需要澄清，应直接让系统从档案中提取答案

                """);

        if (healthProfile != null && !healthProfile.isEmpty()) {
            sb.append("## 用户健康档案（已有的信息，无需追问）\n");
            sb.append(healthProfile).append("\n\n");
        }

        sb.append(String.format("""
                ## 对话历史
                %s

                ## 用户问题
                %s

                ## 输出格式（严格JSON，不要输出其他内容）
                {"needsClarification": true/false, "clarificationType": "...", "question": "...", "options": [...], "missingFields": [...]}
                """, history, question));

        return sb.toString();
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
