package com.healthassistant.module.rag.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final ElasticsearchClient esClient;
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:health_knowledge}")
    private String indexName;

    public VectorSearchService(ElasticsearchClient esClient, EmbeddingModel embeddingModel) {
        this.esClient = esClient;
        this.embeddingModel = embeddingModel;
    }

    public List<RetrievedDocument> search(List<String> queries, int topK) {
        List<RetrievedDocument> results = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (String query : queries) {
            try {
                float[] queryVector = embed(query);
                if (queryVector == null) continue;

                List<Float> queryVectorList = new ArrayList<>();
                for (float v : queryVector) queryVectorList.add(v);

                SearchResponse<Map> response = esClient.search(s -> s
                        .index(indexName)
                        .knn(k -> k
                                .field("content_vector")
                                .queryVector(queryVectorList)
                                .k(topK)
                                .numCandidates(topK * 3)
                        )
                        .size(topK),
                        Map.class);

                response.hits().hits().forEach(hit -> {
                    String docId = hit.id();
                    if (seenIds.add(docId)) {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            double score = hit.score() != null ? hit.score() : 0.0;
                            results.add(toRetrievedDocument(docId, source, score));
                        }
                    }
                });
            } catch (IOException e) {
                log.warn("Vector search failed for query: {}", query, e);
            }
        }

        log.debug("Vector search returned {} unique documents from {} queries", results.size(), queries.size());
        return results;
    }

    private float[] embed(String text) {
        try {
            var request = new EmbeddingRequest(List.of(text), null);
            var response = embeddingModel.call(request);
            if (response.getResults().isEmpty()) return null;
            float[] vector = response.getResults().get(0).getOutput();
            log.debug("Embedding: {} dims for: {}", vector.length, text.substring(0, Math.min(50, text.length())));
            return vector;
        } catch (Exception e) {
            log.error("Embedding failed for: {}", text, e);
            return null;
        }
    }

    private RetrievedDocument toRetrievedDocument(String docId, Map<String, Object> source, Double score) {
        return RetrievedDocument.builder()
                .docId(docId)
                .content(getString(source, "content"))
                .score(score != null ? score : 0.0)
                .source("vector")
                .metadata(source)
                .parentDocId(getString(source, "parent_doc_id"))
                .sectionPath(getString(source, "section_path"))
                .headingLevel(getInt(source, "heading_level", 1))
                .evidenceLevel(getInt(source, "evidence_level", 1))
                .documentType(getString(source, "document_type"))
                .publicationDate(getString(source, "publication_date"))
                .sourceName(getString(source, "source_name"))
                .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }
}
