package com.healthassistant.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotNull(message = "反馈类型不能为空")
    private Integer feedbackType; // 1-thumbs up 0-thumbs down

    private String comment;
}
