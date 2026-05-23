package com.healthassistant.module.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private String messageId;
    private Long sessionId;
    private String parentMessageId;
    private Integer messageType;
    private String contentType;
    private String content;
    private String contentHtml;
    private Integer evidenceLevel;
    private String sources;
    private String clarificationData;
    private Integer feedbackType;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
}
