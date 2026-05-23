package com.healthassistant.module.chat.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.module.chat.dto.FeedbackRequest;
import com.healthassistant.module.chat.entity.ChatMessage;
import com.healthassistant.module.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final ChatMessageRepository messageRepository;

    public FeedbackService(ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        ChatMessage message = messageRepository.findByMessageId(request.getMessageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND, "消息不存在"));

        message.setFeedbackType(request.getFeedbackType());
        if (request.getComment() != null && !request.getComment().isBlank()) {
            message.setFeedbackComment(request.getComment());
        }
        messageRepository.save(message);
    }
}
