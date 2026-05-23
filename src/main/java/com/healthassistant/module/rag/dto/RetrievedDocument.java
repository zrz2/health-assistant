package com.healthassistant.module.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedDocument {

    private String docId;
    private String content;
    private double score;
    private String source; // vector / keyword / hybrid
    private Map<String, Object> metadata;

    // Section-aware fields
    private String parentDocId;
    private String sectionPath;
    private int headingLevel;

    // Evidence & quality
    private int evidenceLevel;
    private String documentType;
    private String publicationDate;
    private String sourceName;
    private List<String> medicalEntities;
}
