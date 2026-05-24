package com.healthassistant.module.chat.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.common.util.IdGenerator;
import com.healthassistant.module.chat.dto.ChatSessionDTO;
import com.healthassistant.module.chat.entity.ChatMessage;
import com.healthassistant.module.chat.entity.ChatSession;
import com.healthassistant.module.chat.repository.ChatMessageRepository;
import com.healthassistant.module.chat.repository.ChatSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public SessionService(ChatSessionRepository sessionRepository,
                          ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ChatSessionDTO createSession(Long userId, String title, String firstMessage) {
        ChatSession session = new ChatSession();
        session.setSessionId(IdGenerator.generateSessionId());
        session.setUserId(userId);
        session.setTitle(title != null ? title : generateTitle(firstMessage));
        sessionRepository.save(session);
        return toDTO(session);
    }

    public Page<ChatSessionDTO> listSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, 1, pageable)
                .map(this::toDTO);
    }

    public ChatSessionDTO getSession(String sessionId, Long userId) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在"));
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return toDTO(session);
    }

    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在"));
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该会话");
        }
        session.setStatus(0);
        sessionRepository.save(session);
    }

    @Transactional
    public void incrementMessageCount(Long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setMessageCount(session.getMessageCount() + 1);
            sessionRepository.save(session);
        });
    }

    private ChatSessionDTO toDTO(ChatSession session) {
        List<ChatMessage> messages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId());
        String lastMessage = messages.isEmpty() ? null :
                messages.get(messages.size() - 1).getContent();

        return ChatSessionDTO.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .status(session.getStatus())
                .messageCount(session.getMessageCount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .lastMessage(lastMessage != null && lastMessage.length() > 100
                        ? lastMessage.substring(0, 100) : lastMessage)
                .build();
    }

    private String generateTitle(String message) {
        if (message == null || message.isEmpty()) return "新的对话";
        return message.length() > 20 ? message.substring(0, 20) + "..." : message;
    }
}
