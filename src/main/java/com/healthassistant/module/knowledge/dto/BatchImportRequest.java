package com.healthassistant.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BatchImportRequest {

    @NotBlank(message = "来源名称不能为空")
    private String sourceName;

    @NotEmpty(message = "文档列表不能为空")
    private List<DocumentInput> documents;

    @Data
    public static class DocumentInput {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        private String url;
        private LocalDate publicationDate;
    }
}
