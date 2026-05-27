package com.healthassistant.module.rag.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import com.healthassistant.module.rag.dto.SourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);
    private static final int MAX_CONTEXT_LENGTH = 3000;

    private final ElasticsearchClient esClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:health_knowledge}")
    private String indexName;

    public ContextBuilder(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * Build structured context from retrieved documents for LLM prompt.
     */
    public String build(List<RetrievedDocument> documents) {
        if (documents.isEmpty()) {
            return "暂无相关医学知识。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是与用户问题相关的医学知识，请基于这些信息回答：\n\n");

        int totalLength = 0;
        int sourceCount = 0;

        for (int i = 0; i < documents.size(); i++) {
            RetrievedDocument doc = documents.get(i);
            String section = formatDocument(i + 1, doc);

            totalLength += section.length();
            if (totalLength > MAX_CONTEXT_LENGTH) {
                break;
            }

            sb.append(section).append("\n");
            sourceCount++;
        }

        sb.append("\n--- 来源引用 ---\n");
        for (int i = 0; i < sourceCount; i++) {
            RetrievedDocument doc = documents.get(i);
            sb.append(formatSource(i + 1, doc));
        }

        return sb.toString();
    }

    /**
     * Build context with parent document expansion.
     * For each retrieved chunk, query sibling chunks with the same parent_doc_id
     * and merge them into complete sections for richer context.
     */
    public String buildWithParentContext(List<RetrievedDocument> documents) {
        if (documents.isEmpty()) {
            return "暂无相关医学知识。";
        }

        // Group retrieved docs by parent_doc_id
        Map<String, List<RetrievedDocument>> byParent = documents.stream()
                .filter(d -> d.getParentDocId() != null && !d.getParentDocId().isBlank())
                .collect(Collectors.groupingBy(RetrievedDocument::getParentDocId));

        if (byParent.isEmpty()) {
            return build(documents);
        }

        // For each parent doc, fetch all sibling chunks from ES
        Map<String, List<RetrievedDocument>> expandedDocs = new LinkedHashMap<>();
        Set<String> seenDocIds = new HashSet<>();
        for (RetrievedDocument doc : documents) {
            seenDocIds.add(doc.getDocId());
        }

        for (String parentDocId : byParent.keySet()) {
            try {
                List<RetrievedDocument> siblings = fetchSiblingChunks(parentDocId);
                // Merge with existing documents, deduplicate
                List<RetrievedDocument> merged = new ArrayList<>();
                for (RetrievedDocument sib : siblings) {
                    if (seenDocIds.add(sib.getDocId())) {
                        merged.add(sib);
                    }
                }
                if (!merged.isEmpty()) {
                    expandedDocs.put(parentDocId, merged);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch siblings for parent doc {}: {}", parentDocId, e.getMessage());
            }
        }

        if (expandedDocs.isEmpty()) {
            return build(documents);
        }

        // Build context: first include expanded sibling context, then individual hits
        StringBuilder sb = new StringBuilder();
        sb.append("以下是与用户问题相关的医学知识，请基于这些信息回答：\n\n");

        int totalLength = 0;
        int refIndex = 0;

        // Add expanded parent sections first (richer context)
        for (var entry : expandedDocs.entrySet()) {
            List<RetrievedDocument> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparing(d -> d.getSectionPath() != null ? d.getSectionPath() : ""))
                    .toList();

            for (RetrievedDocument doc : sorted) {
                refIndex++;
                String section = formatDocument(refIndex, doc);
                totalLength += section.length();
                if (totalLength > MAX_CONTEXT_LENGTH) {
                    break;
                }
                sb.append(section).append("\n");
            }
        }

        // Add remaining original documents not already expanded
        for (RetrievedDocument doc : documents) {
            if (!expandedDocs.containsKey(doc.getParentDocId())) {
                refIndex++;
                String section = formatDocument(refIndex, doc);
                totalLength += section.length();
                if (totalLength > MAX_CONTEXT_LENGTH) {
                    break;
                }
                sb.append(section).append("\n");
            }
        }

        // Source references
        sb.append("\n--- 来源引用 ---\n");
        Set<String> seenSources = new HashSet<>();
        int srcIdx = 0;
        for (var entry : expandedDocs.entrySet()) {
            for (RetrievedDocument doc : entry.getValue()) {
                String srcName = doc.getSourceName();
                if (srcName != null && seenSources.add(srcName)) {
                    srcIdx++;
                    sb.append(formatSource(srcIdx, doc));
                }
            }
        }
        for (RetrievedDocument doc : documents) {
            if (!expandedDocs.containsKey(doc.getParentDocId())) {
                String srcName = doc.getSourceName();
                if (srcName != null && seenSources.add(srcName)) {
                    srcIdx++;
                    sb.append(formatSource(srcIdx, doc));
                }
            }
        }

        log.debug("Context built with {} expanded parent docs, {} total refs", expandedDocs.size(), refIndex);
        return sb.toString();
    }

    /**
     * Fetch all sibling chunks from ES that share the same parent_doc_id.
     */
    private List<RetrievedDocument> fetchSiblingChunks(String parentDocId) throws IOException {
        SearchResponse<Map> response = esClient.search(s -> s
                        .index(indexName)
                        .query(q -> q.term(t -> t.field("parent_doc_id").value(parentDocId)))
                        .size(50),
                Map.class);

        List<RetrievedDocument> siblings = new ArrayList<>();
        response.hits().hits().forEach(hit -> {
            Map<String, Object> source = hit.source();
            if (source != null) {
                siblings.add(RetrievedDocument.builder()
                        .docId(hit.id())
                        .content(getString(source, "content"))
                        .score(hit.score() != null ? hit.score() : 0.0)
                        .source("parent_expansion")
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

        log.debug("Fetched {} sibling chunks for parent doc {}", siblings.size(), parentDocId);
        return siblings;
    }

    private String formatDocument(int index, RetrievedDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("【文献").append(index).append("】");

        if (doc.getSectionPath() != null && !doc.getSectionPath().isEmpty()) {
            sb.append(" [").append(doc.getSectionPath()).append("]");
        }
        if (doc.getEvidenceLevel() > 0) {
            sb.append(" (证据等级:").append(doc.getEvidenceLevel()).append("/5)");
        }
        sb.append("\n").append(doc.getContent()).append("\n");
        return sb.toString();
    }

    private String formatSource(int index, RetrievedDocument doc) {
        return String.format("[%d] %s | %s | 证据等级:%d\n",
                index,
                doc.getSourceName() != null ? doc.getSourceName() : "未知来源",
                doc.getPublicationDate() != null ? doc.getPublicationDate() : "N/A",
                doc.getEvidenceLevel());
    }

    /**
     * Extract up to 5 unique source references from retrieved documents
     * as structured objects for frontend display.
     */
    public List<SourceInfo> extractSources(List<RetrievedDocument> documents) {
        return documents.stream()
                .filter(d -> d.getSourceName() != null && !d.getSourceName().isBlank())
                .collect(Collectors.toMap(
                        RetrievedDocument::getSourceName, // key: source name
                        d -> {
                            String title = d.getContent() != null && !d.getContent().isBlank()
                                    ? (d.getContent().length() > 80
                                            ? d.getContent().substring(0, 80).replace("\n", " ") + "..."
                                            : d.getContent().replace("\n", " "))
                                    : d.getSourceName();
                            return new SourceInfo(title, null, d.getSourceName());
                        },
                        (existing, replacement) -> existing, // dedup by source name
                        LinkedHashMap::new))
                .values().stream()
                .limit(5)
                .collect(Collectors.toList());
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
