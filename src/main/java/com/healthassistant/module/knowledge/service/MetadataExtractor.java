package com.healthassistant.module.knowledge.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetadataExtractor {

    private static final Map<String, String> DOC_TYPE_MAP = Map.of(
            "指南", "clinical_guideline",
            "临床指南", "clinical_guideline",
            "教科书", "medical_textbook",
            "教材", "medical_textbook",
            "论文", "research_paper",
            "研究", "research_paper",
            "药品", "drug_manual",
            "说明书", "drug_manual",
            "百科", "health_encyclopedia",
            "科普", "health_encyclopedia"
    );

    private final MedicalEntityExtractor entityExtractor;

    public MetadataExtractor(MedicalEntityExtractor entityExtractor) {
        this.entityExtractor = entityExtractor;
    }

    public ExtractedMetadata extract(String content, String title, String sourceName) {
        List<String> entities = entityExtractor.extractEntities(content);
        String docType = guessDocumentType(title, content, sourceName);
        int headingLevel = guessHeadingLevel(title);

        return new ExtractedMetadata(docType, entities, headingLevel);
    }

    private String guessDocumentType(String title, String sourceName, String _content) {
        String combined = (title + " " + (sourceName != null ? sourceName : "")).toLowerCase();
        for (var entry : DOC_TYPE_MAP.entrySet()) {
            if (combined.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "health_encyclopedia"; // default for health assistant
    }

    private int guessHeadingLevel(String title) {
        if (title == null) return 1;
        if (title.matches(".*第[一二三四五六七八九十]+章.*")) return 1;
        if (title.matches(".*第[一二三四五六七八九十]+节.*")) return 2;
        if (title.matches(".*\\d+\\.\\d+.*")) return 3;
        return 1;
    }

    public record ExtractedMetadata(String documentType, List<String> medicalEntities,
                                    int headingLevel) {}
}
