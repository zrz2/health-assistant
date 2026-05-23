package com.healthassistant.module.knowledge.dto;

import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeItemResponse {

    private Long id;
    private String docId;
    private String title;
    private String documentType;
    private String sourceName;
    private String sourceUrl;
    private Integer evidenceLevel;
    private LocalDate publicationDate;
    private Integer status;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeItemResponse from(KnowledgeItem item) {
        return KnowledgeItemResponse.builder()
                .id(item.getId())
                .docId(item.getDocId())
                .title(item.getTitle())
                .documentType(item.getDocumentType())
                .sourceName(item.getSourceName())
                .sourceUrl(item.getSourceUrl())
                .evidenceLevel(item.getEvidenceLevel())
                .publicationDate(item.getPublicationDate())
                .status(item.getStatus())
                .chunkCount(item.getChunkCount())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
