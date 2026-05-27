package com.healthassistant.module.knowledge.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 从文档内容/标题中提取元数据：文档类型、医学实体、标题层级。
 * 注意：当用于 chunk 粒度时，content 应传入 chunk 文本，标题保持原知识条目标题。
 */
@Service
public class MetadataExtractor {

    // 关键词 → 文档类型映射
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

    /**
     * 从给定内容中提取元数据。
     * @param content    文档或 chunk 的文本
     * @param title      知识条目标题（可传入原文档标题）
     * @param sourceName 来源名称（如 WHO、中国疾控中心）
     */
    public ExtractedMetadata extract(String content, String title, String sourceName) {
        List<String> entities = entityExtractor.extractEntities(content);
        String docType = guessDocumentType(title, sourceName, content);
        int headingLevel = guessHeadingLevel(title);
        return new ExtractedMetadata(docType, entities, headingLevel);
    }

    private String guessDocumentType(String title, String sourceName, String content) {
        String combined = (title + " " + (sourceName != null ? sourceName : "")).toLowerCase();
        for (var entry : DOC_TYPE_MAP.entrySet()) {
            if (combined.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 默认返回健康百科
        return "health_encyclopedia";
    }

    private int guessHeadingLevel(String title) {
        if (title == null) return 1;
        if (title.matches(".*第[一二三四五六七八九十]+章.*")) return 1;
        if (title.matches(".*第[一二三四五六七八九十]+节.*")) return 2;
        if (title.matches(".*\\d+\\.\\d+.*")) return 3;
        return 1;
    }

    public record ExtractedMetadata(String documentType, List<String> medicalEntities, int headingLevel) {}
}