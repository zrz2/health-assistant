package com.healthassistant.module.rag.service;

import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final int RRF_K = 60;

    public List<RetrievedDocument> merge(List<RetrievedDocument> vectorDocs,
                                          List<RetrievedDocument> keywordDocs,
                                          int topK) {
        // Build RRF ranking map: docId -> RRF score
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, RetrievedDocument> docMap = new LinkedHashMap<>();

        // RRF for vector results
        for (int i = 0; i < vectorDocs.size(); i++) {
            RetrievedDocument doc = vectorDocs.get(i);
            String id = doc.getDocId();
            rrfScores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
            docMap.putIfAbsent(id, doc);
        }

        // RRF for keyword results
        for (int i = 0; i < keywordDocs.size(); i++) {
            RetrievedDocument doc = keywordDocs.get(i);
            String id = doc.getDocId();
            rrfScores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
            if (!docMap.containsKey(id)) {
                docMap.put(id, doc);
            }
        }

        // Apply evidence level boost
        for (RetrievedDocument doc : docMap.values()) {
            double evidenceBoost = doc.getEvidenceLevel() * 0.1;
            rrfScores.merge(doc.getDocId(), evidenceBoost, Double::sum);
        }

        // Apply recency boost (2023+ documents get bonus)
        for (RetrievedDocument doc : docMap.values()) {
            String date = doc.getPublicationDate();
            if (date != null && date.compareTo("2023") >= 0) {
                rrfScores.merge(doc.getDocId(), 0.02, Double::sum);
            }
        }

        // Sort by RRF score descending and take top K
        List<RetrievedDocument> merged = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    RetrievedDocument doc = docMap.get(e.getKey());
                    doc.setScore(e.getValue());
                    doc.setSource("hybrid");
                    return doc;
                })
                .collect(Collectors.toList());

        log.debug("RRF merged {} vector + {} keyword -> {} hybrid (top {})",
                vectorDocs.size(), keywordDocs.size(), merged.size(), topK);
        return merged;
    }
}
