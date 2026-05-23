package com.healthassistant.module.knowledge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class EvidenceGradingService {

    @Value("${app.evidence.weights.source:0.5}")
    private double sourceWeight;

    @Value("${app.evidence.weights.time:0.3}")
    private double timeWeight;

    @Value("${app.evidence.weights.type:0.2}")
    private double typeWeight;

    private static final Map<String, Integer> SOURCE_LEVELS = Map.of(
            "clinical_guideline", 5,
            "research_paper", 4,
            "medical_textbook", 4,
            "drug_manual", 4,
            "health_encyclopedia", 2
    );

    /**
     * Calculate evidence level (1-5) based on source type, publication date, and document type.
     */
    public int calculateEvidenceLevel(String documentType, LocalDate publicationDate,
                                      String sourceName) {
        int sourceScore = SOURCE_LEVELS.getOrDefault(documentType, 2);
        int timeScore = calculateTimeScore(publicationDate);
        int typeScore = calculateTypeScore(documentType);

        double weighted = sourceScore * sourceWeight + timeScore * timeWeight + typeScore * typeWeight;
        return (int) Math.round(Math.max(1, Math.min(5, weighted)));
    }

    private int calculateTimeScore(LocalDate publicationDate) {
        if (publicationDate == null) return 3;
        long years = ChronoUnit.YEARS.between(publicationDate, LocalDate.now());
        if (years <= 2) return 5;
        if (years <= 5) return 4;
        if (years <= 10) return 3;
        return 2;
    }

    private int calculateTypeScore(String documentType) {
        return switch (documentType != null ? documentType : "") {
            case "clinical_guideline" -> 5;
            case "medical_textbook" -> 4;
            case "drug_manual" -> 4;
            case "research_paper" -> 4;
            case "health_encyclopedia" -> 2;
            default -> 3;
        };
    }
}
