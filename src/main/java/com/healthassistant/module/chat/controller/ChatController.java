package com.healthassistant.module.chat.controller;

import com.healthassistant.common.result.Result;
import com.healthassistant.module.chat.dto.*;
import com.healthassistant.module.chat.service.*;
import com.healthassistant.module.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "对话管理")
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final SessionService sessionService;
    private final ChatService chatService;
    private final ClarificationService clarificationService;
    private final FeedbackService feedbackService;

    public ChatController(SessionService sessionService,
                          ChatService chatService,
                          ClarificationService clarificationService,
                          FeedbackService feedbackService) {
        this.sessionService = sessionService;
        this.chatService = chatService;
        this.clarificationService = clarificationService;
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "创建会话")
    @PostMapping("/sessions")
    public Result<ChatSessionDTO> createSession(@Valid @RequestBody ChatSessionCreateRequest request) {
        Long userId = getCurrentUserId();
        return Result.success(sessionService.createSession(userId, request.getTitle(), request.getFirstMessage()));
    }

    @Operation(summary = "会话列表")
    @GetMapping("/sessions")
    public Result<Page<ChatSessionDTO>> listSessions(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.success(Page.empty());
        }
        return Result.success(sessionService.listSessions(userId, pageable));
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionDTO> getSession(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        return Result.success(sessionService.getSession(sessionId, userId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        sessionService.deleteSession(sessionId, userId);
        return Result.success();
    }

    @Operation(summary = "发送消息（SSE流式）")
    @PostMapping("/messages")
    public SseEmitter sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        Long userId = getCurrentUserId();
        return chatService.sendMessage(userId, request.getSessionId(), request.getContent(),
                request.isSkipClarification());
    }

    @Operation(summary = "获取消息历史")
    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatMessageDTO>> getMessages(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        return Result.success(chatService.getMessageHistory(sessionId, userId));
    }

    @Operation(summary = "回答澄清问题")
    @PostMapping("/clarify")
    public Result<String> answerClarification(@Valid @RequestBody ClarificationAnswerRequest request) {
        String rewritten = clarificationService.answerClarification(
                request.getClarificationId(), request.getAnswer());
        return Result.success("已收到补充信息", rewritten);
    }

    @Operation(summary = "提交反馈")
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.submitFeedback(request);
        return Result.success();
    }

    @Operation(summary = "获取推荐问题")
    @GetMapping("/suggested-questions")
    public Result<List<String>> getSuggestedQuestions() {
        return Result.success(List.of(
                "头痛应该挂什么科？",
                "高血压患者饮食需要注意什么？",
                "感冒药和消炎药可以一起吃吗？",
                "如何判断是否发烧？"
        ));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
