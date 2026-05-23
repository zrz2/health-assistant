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
public class ClarificationDTO {

    private String clarificationId;
    private String sessionId;
    private String originalMessageId;
    private String clarificationType;
    private String question;
    private String options;
    private String missingFields;
    private Integer status;
    private LocalDateTime createdAt;
}
