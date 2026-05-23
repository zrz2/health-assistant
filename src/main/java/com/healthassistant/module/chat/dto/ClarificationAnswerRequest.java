package com.healthassistant.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClarificationAnswerRequest {

    @NotBlank(message = "澄清ID不能为空")
    private String clarificationId;

    @NotBlank(message = "回答不能为空")
    @Size(max = 2000, message = "回答最长2000位")
    private String answer;
}
