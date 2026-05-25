package com.healthassistant.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息最长2000位")
    private String content;

    /** 跳过澄清检测，用于已澄清后的改写问题 */
    private boolean skipClarification;
}
