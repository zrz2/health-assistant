package com.healthassistant.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveWordRequest {

    @NotBlank(message = "敏感词不能为空")
    @Size(max = 100, message = "敏感词长度不能超过100")
    private String word;
}
