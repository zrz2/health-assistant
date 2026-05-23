package com.healthassistant.module.rag.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KeywordSearchService {

    private static final Logger log = LoggerFactory.getLogger(KeywordSearchService.class);

    private final ElasticsearchClient esClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:health_knowledge}")
    private String indexName;

    public KeywordSearchService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public List<RetrievedDocument> search(String query, int topK) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(indexName)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .match(ma -> ma
                                                    .field("content")
                                                    .query(query)
                                                    .operator(Operator.Or)
                                            )
                                    )
                                    .filter(f -> f
                                            .range(r -> r
                                                    .field("evidence_level")
                                                    .gte(JsonData.of(3.0))
                                            )
                                    )
                            )
                    )
                    .size(topK),
                    Map.class
            );

            List<RetrievedDocument> results = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    results.add(RetrievedDocument.builder()
                            .docId(hit.id())
                            .content(getString(source, "content"))
                            .score(hit.score() != null ? hit.score() : 0.0)
                            .source("keyword")
                            .metadata(source)
                            .parentDocId(getString(source, "parent_doc_id"))
                            .sectionPath(getString(source, "section_path"))
                            .headingLevel(getInt(source, "heading_level", 1))
                            .evidenceLevel(getInt(source, "evidence_level", 1))
                            .documentType(getString(source, "document_type"))
                            .publicationDate(getString(source, "publication_date"))
                            .sourceName(getString(source, "source_name"))
                            .build());
                }
            });

            log.debug("Keyword search returned {} documents for: {}", results.size(), query);
            return results;
        } catch (IOException e) {
            log.error("Keyword search failed", e);
            return List.of();
        }
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

    private double getDouble(Map<String, Object> map, String key, double def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return def;
    }
}
