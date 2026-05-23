package com.healthassistant.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSessionCreateRequest {

    @Size(max = 200, message = "标题最长200位")
    private String title;

    @NotBlank(message = "首条消息不能为空")
    @Size(max = 2000, message = "消息最长2000位")
    private String firstMessage;
}
