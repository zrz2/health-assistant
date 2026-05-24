package com.healthassistant.module.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthassistant.common.constant.Constants;
import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.common.util.IdGenerator;
import com.healthassistant.module.admin.service.SensitiveWordService;
import com.healthassistant.module.chat.dto.ChatMessageDTO;
import com.healthassistant.module.chat.entity.ChatMessage;
import com.healthassistant.module.chat.entity.ChatSession;
import com.healthassistant.module.chat.entity.ClarificationRecord;
import com.healthassistant.module.chat.repository.ChatMessageRepository;
import com.healthassistant.module.chat.repository.ChatSessionRepository;
import com.healthassistant.module.user.entity.HealthRecord;
import com.healthassistant.module.user.repository.HealthRecordRepository;
import com.healthassistant.module.rag.service.RagService;
import com.healthassistant.common.util.RetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SessionService sessionService;
    private final ClarificationService clarificationService;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final RagService ragService;
    private final SensitiveWordService sensitiveWordService;
    private final HealthRecordRepository healthRecordRepository;
    private final ExecutorService chatExecutor;

    @Value("${app.chat.max-history:10}")
    private int maxHistory;

    @Value("${app.chat.stream-timeout:30000}")
    private long streamTimeout;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       SessionService sessionService,
                       ClarificationService clarificationService,
                       ChatClient.Builder chatClientBuilder,
                       ObjectMapper objectMapper,
                       RagService ragService,
                       SensitiveWordService sensitiveWordService,
                       HealthRecordRepository healthRecordRepository,
                       ExecutorService chatExecutor) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.sessionService = sessionService;
        this.clarificationService = clarificationService;
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
        this.ragService = ragService;
        this.sensitiveWordService = sensitiveWordService;
        this.healthRecordRepository = healthRecordRepository;
        this.chatExecutor = chatExecutor;
    }

    @Transactional
    public SseEmitter sendMessage(Long userId, String sessionIdStr, String content) {
        ChatSession session = sessionRepository.findBySessionId(sessionIdStr)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在"));

        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }

        // Save user message (quick DB ops)
        ChatMessage userMsg = saveMessage(session.getId(), null, 1, content);
        sessionService.incrementMessageCount(session.getId());

        // Get history and health profile
        String history = buildHistory(session.getId());
        String healthProfile = buildHealthProfile(userId);

        // Create emitter with generous timeout
        SseEmitter emitter = new SseEmitter(streamTimeout);

        // Send immediate "processing" event so frontend shows feedback right away
        sendEvent(emitter, "processing", new ChatEvent("processing", null,
                "正在分析您的问题，请稍候...", null, null, null));

        // Run heavy LLM processing asynchronously so the emitter connects immediately
        final Long sessionId = session.getId();
        final String msgId = userMsg.getMessageId();
        final String finalHealthProfile = healthProfile;
        CompletableFuture.runAsync(() -> {
            try {
                if (sensitiveWordService.containsSensitiveWord(content)) {
                    sendEvent(emitter, "error",
                            new ChatEvent("error", null, "消息包含违规内容，无法回答", null, null, null));
                    emitter.complete();
                    return;
                }

                ClarificationService.ClarificationResult clarification = clarificationService
                        .checkClarification(content, history);

                if (clarification.needsClarification()) {
                    handleClarification(emitter, session, msgId, clarification);
                } else {
                    handleStreamResponse(emitter, sessionId, content, history, finalHealthProfile, msgId);
                }
            } catch (Exception e) {
                log.error("Chat processing error for session {}", sessionId, e);
                String errMsg = e instanceof BusinessException ? e.getMessage()
                        : "抱歉，处理您的问题时出错了，请重试。";
                sendEvent(emitter, "error",
                        new ChatEvent("error", null, errMsg, null, null, null));
                emitter.complete();
            }
        }, chatExecutor);

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String eventName, ChatEvent event) {
        try {
            emitter.send(SseEmitter.event().name(eventName)
                    .data(objectMapper.writeValueAsString(event)));
        } catch (IOException e) {
            log.warn("Failed to send SSE event '{}': {}", eventName, e.getMessage());
        }
    }

    private void handleClarification(SseEmitter emitter, ChatSession session,
                                      String messageId,
                                      ClarificationService.ClarificationResult result) {
        // Save clarification record
        ClarificationRecord record = clarificationService.createClarification(
                session.getId(), messageId, result);

        // Save clarification message
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(IdGenerator.generateMessageId());
        msg.setSessionId(session.getId());
        msg.setParentMessageId(messageId);
        msg.setMessageType(4); // CLARIFICATION
        msg.setContent(result.question());
        msg.setClarificationData(buildClarificationData(record.getClarificationId(), result));
        messageRepository.save(msg);
        sessionService.incrementMessageCount(session.getId());

        try {
            String eventData = objectMapper.writeValueAsString(
                    new ChatEvent("clarification",
                            record.getClarificationId(),
                            result.question(),
                            result.options(),
                            result.clarificationType(),
                            null));
            emitter.send(SseEmitter.event().name("clarification").data(eventData));
            emitter.complete();
        } catch (IOException e) {
            log.error("SSE send error", e);
            emitter.completeWithError(e);
        }
    }

    private void handleStreamResponse(SseEmitter emitter, Long sessionId,
                                       String question, String history, String healthProfile,
                                       String parentMessageId) {
        // Try RAG augmentation
        RagService.RagResult ragResult = ragService.augmentPrompt(question, history);
        String prompt;
        String sources = null;

        if (ragResult != null) {
            prompt = ragResult.prompt();
            if (!ragResult.sources().isEmpty()) {
                sources = String.join(", ", ragResult.sources());
            }
            log.info("Using RAG-augmented prompt with {} sources", ragResult.sources().size());
        } else {
            prompt = buildQaPrompt(question, history, healthProfile);
        }

        // Inject health profile into RAG prompt
        if (healthProfile != null && !healthProfile.isEmpty()) {
            if (ragResult != null) {
                prompt = prompt.replace("## 回答规则", healthProfile + "\n\n## 回答规则");
            }
            log.info("Health profile injected into prompt for user session {}", sessionId);
        }

        final String finalPrompt = prompt;
        final String finalSources = sources;
        StringBuilder fullContent = new StringBuilder();

        Flux<String> flux = Flux.defer(() -> {
                    ChatClient chatClient = chatClientBuilder.build();
                    return chatClient.prompt()
                            .user(finalPrompt)
                            .stream()
                            .content();
                })
                .retryWhen(RetryUtils.fluxRetry("ChatStream"))
                .timeout(java.time.Duration.ofMillis(streamTimeout));

        flux.subscribe(
                chunk -> {
                    try {
                        fullContent.append(chunk);
                        String eventData = objectMapper.writeValueAsString(
                                new ChatEvent("message", null, chunk, null, null, null));
                        emitter.send(SseEmitter.event().name("message").data(eventData));
                    } catch (IOException e) {
                        log.error("SSE send error", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("Stream error", error);
                    saveAssistantMessage(sessionId, parentMessageId, fullContent.toString(), true);
                    emitter.completeWithError(error);
                },
                () -> {
                    saveAssistantMessage(sessionId, parentMessageId, fullContent.toString(), false);
                    try {
                        String doneData = objectMapper.writeValueAsString(
                                new ChatEvent("done", null, null, null, null, finalSources));
                        emitter.send(SseEmitter.event().name("done").data(doneData));
                    } catch (IOException e) {
                        log.error("SSE done send error", e);
                    }
                    emitter.complete();
                }
        );
    }

    @Transactional
    public void saveAssistantMessage(Long sessionId, String parentMessageId,
                                      String content, boolean isError) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(IdGenerator.generateMessageId());
        msg.setSessionId(sessionId);
        msg.setParentMessageId(parentMessageId);
        msg.setMessageType(2); // ASSISTANT
        msg.setContent(isError ? "抱歉，回答生成失败，请重试。" : content);
        messageRepository.save(msg);
        sessionService.incrementMessageCount(sessionId);
    }

    public List<ChatMessageDTO> getMessageHistory(String sessionId, Long userId) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在"));

        if (!Objects.equals(session.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ChatMessage saveMessage(Long sessionId, String parentMessageId,
                                     int messageType, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(IdGenerator.generateMessageId());
        msg.setSessionId(sessionId);
        msg.setParentMessageId(parentMessageId);
        msg.setMessageType(messageType);
        msg.setContent(content);
        messageRepository.save(msg);
        return msg;
    }

    private String buildHistory(Long sessionId) {
        List<ChatMessage> messages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);

        if (messages.size() > maxHistory * 2) {
            messages = messages.subList(messages.size() - maxHistory * 2, messages.size());
        }

        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            String role = msg.getMessageType() == 1 ? "用户" : "助手";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String buildQaPrompt(String question, String history, String healthProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是一个专业的医疗健康助手。请根据以下规则回答用户问题：

                ## 核心规则
                1. 你不是医生，不能进行诊断。回答仅供参考，不能替代专业医疗建议。
                2. 回答应专业、客观、严谨。
                3. 如果用户描述紧急症状（胸痛、严重出血、呼吸困难等），建议立即就医。
                4. 对于不确定的问题，诚实说明并建议咨询医生。
                5. 如果提供了用户健康档案，应结合档案信息给出个性化建议。

                """);

        if (healthProfile != null && !healthProfile.isEmpty()) {
            sb.append(healthProfile).append("\n\n");
        }

        sb.append(String.format("""
                ## 对话历史
                %s

                ## 当前问题
                %s

                ## 免责声明
                %s
                """, history, question, Constants.MEDICAL_DISCLAIMER));

        return sb.toString();
    }

    private String buildHealthProfile(Long userId) {
        if (userId == null) return null;

        return healthRecordRepository.findByUserId(userId)
                .map(record -> {
                    log.debug("Found health record for userId={}", userId);
                    StringBuilder sb = new StringBuilder();
                    sb.append("## 用户健康档案\n");
                    sb.append("以下是当前用户的健康信息，回答健康相关问题时必须结合这些信息给出个性化建议：\n");

                    if (record.getAge() != null) sb.append("- 年龄: ").append(record.getAge()).append("岁\n");
                    if (record.getGender() != null) {
                        String gender = switch (record.getGender()) {
                            case 1 -> "男";
                            case 0 -> "女";
                            default -> "其他";
                        };
                        sb.append("- 性别: ").append(gender).append("\n");
                    }
                    if (record.getHeight() != null) sb.append("- 身高: ").append(record.getHeight()).append("cm\n");
                    if (record.getWeight() != null) sb.append("- 体重: ").append(record.getWeight()).append("kg\n");
                    if (record.getBloodType() != null && !record.getBloodType().isBlank())
                        sb.append("- 血型: ").append(record.getBloodType()).append("\n");
                    if (record.getMedicalHistory() != null && !record.getMedicalHistory().isBlank())
                        sb.append("- 既往病史: ").append(record.getMedicalHistory()).append("\n");
                    if (record.getAllergies() != null && !record.getAllergies().isBlank())
                        sb.append("- 过敏史: ").append(record.getAllergies()).append("\n");
                    if (record.getChronicDiseases() != null && !record.getChronicDiseases().isBlank())
                        sb.append("- 慢性病: ").append(record.getChronicDiseases()).append("\n");
                    if (record.getCurrentMedications() != null && !record.getCurrentMedications().isBlank())
                        sb.append("- 当前用药: ").append(record.getCurrentMedications()).append("\n");

                    return sb.toString();
                })
                .orElse(null);
    }

    private String buildClarificationData(String clarificationId,
                                           ClarificationService.ClarificationResult result) {
        try {
            return objectMapper.writeValueAsString(
                    new ClarificationEventData(clarificationId, result.clarificationType(),
                            result.question(), result.options(), result.missingFields()));
        } catch (IOException e) {
            return "{}";
        }
    }

    private ChatMessageDTO toDTO(ChatMessage msg) {
        return ChatMessageDTO.builder()
                .messageId(msg.getMessageId())
                .sessionId(msg.getSessionId())
                .parentMessageId(msg.getParentMessageId())
                .messageType(msg.getMessageType())
                .contentType(msg.getContentType())
                .content(msg.getContent())
                .contentHtml(msg.getContentHtml())
                .evidenceLevel(msg.getEvidenceLevel())
                .sources(msg.getSources())
                .clarificationData(msg.getClarificationData())
                .feedbackType(msg.getFeedbackType())
                .tokensUsed(msg.getTokensUsed())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    public record ChatEvent(String type, String clarificationId, String content,
                             String options, String clarificationType, String sources) {}

    public record ClarificationEventData(String clarificationId, String clarificationType,
                                          String question, String options, String missingFields) {}
}
