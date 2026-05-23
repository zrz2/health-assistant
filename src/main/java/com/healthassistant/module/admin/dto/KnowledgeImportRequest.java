package com.healthassistant.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeImportRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String documentType;

    @NotBlank(message = "来源名称不能为空")
    private String sourceName;

    private String sourceUrl;

    private String publicationDate;

    private Integer evidenceLevel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Article {
        @NotBlank private String title;
        @NotBlank private String content;
        private String documentType;
        private String sourceUrl;
        private String publicationDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchImportRequest {
        @NotBlank(message = "来源名称不能为空")
        private String sourceName;
        private String documentType;
        private List<Article> articles;
    }
}
