package com.healthassistant.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KnowledgeItemRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 500)
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String documentType;

    private String sourceName;

    private String sourceUrl;

    private LocalDate publicationDate;
}
