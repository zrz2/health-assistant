package com.healthassistant.module.knowledge.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.repository.KnowledgeItemRepository;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EmbeddingPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingPipelineService.class);

    private final KnowledgeItemRepository itemRepository;
    private final TextChunker textChunker;
    private final MetadataExtractor metadataExtractor;
    private final EvidenceGradingService evidenceGradingService;
    private final EmbeddingModel embeddingModel;
    private final ElasticsearchClient esClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:health_knowledge}")
    private String indexName;

    public EmbeddingPipelineService(KnowledgeItemRepository itemRepository,
                                    TextChunker textChunker,
                                    MetadataExtractor metadataExtractor,
                                    EvidenceGradingService evidenceGradingService,
                                    EmbeddingModel embeddingModel,
                                    ElasticsearchClient esClient) {
        this.itemRepository = itemRepository;
        this.textChunker = textChunker;
        this.metadataExtractor = metadataExtractor;
        this.evidenceGradingService = evidenceGradingService;
        this.embeddingModel = embeddingModel;
        this.esClient = esClient;
    }

    @Transactional
    public int indexKnowledgeItem(String docId) {
        KnowledgeItem item = itemRepository.findByDocId(docId)
                .orElseThrow(() -> new RuntimeException("Knowledge item not found: " + docId));

        return indexKnowledgeItem(item);
    }

    public int indexKnowledgeItem(KnowledgeItem item) {
        log.info("Indexing knowledge item: {} ({})", item.getDocId(), item.getTitle());

        // 1. Medical section-aware chunking
        TextChunker.ChunkResult chunks = textChunker.chunkByMedicalSections(
                item.getContent(), item.getDocId());

        if (chunks.searchChunks().isEmpty()) {
            log.warn("No chunks produced for document: {}", item.getDocId());
            return 0;
        }

        // 提取全文元数据（用于整个文档的全局信息，仍保留，但每个 chunk 可单独提取实体）
        MetadataExtractor.ExtractedMetadata globalMetadata  = metadataExtractor.extract(
                item.getContent(), item.getTitle(), item.getSourceName());

        // Use scraper-provided evidence level and document type if available;
        // otherwise calculate from content analysis
        int evidenceLevel = resolveEvidenceLevel(item, globalMetadata);
        String documentType = resolveDocumentType(item, globalMetadata);

        // ---- 索引 search chunks ----
        List<BulkOperation> searchOps = new ArrayList<>();
        int indexedSearch = 0;

        for (TextChunker.Chunk chunk : chunks.searchChunks()) {
            // 为每个 chunk 单独提取实体（可选，基于 chunk.content()）
            MetadataExtractor.ExtractedMetadata chunkMetadata = metadataExtractor.extract(
                    chunk.content(), item.getTitle(), item.getSourceName());

            float[] vector = embed(chunk.content());
            if (vector == null) continue;

            Map<String, Object> doc = buildEsDocumentForSearchChunk(
                    chunk, item, chunkMetadata, evidenceLevel, documentType, vector);
            searchOps.add(BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(chunk.chunkId())
                            .document(doc))));
            indexedSearch++;
        }

        if (!searchOps.isEmpty()) {
            try {
                esClient.bulk(BulkRequest.of(b -> b.operations(searchOps)));
                log.info("Indexed {} search chunks for doc {}", indexedSearch, item.getDocId());
            } catch (Exception e) {
                log.error("Search chunk indexing failed: {}", e.getMessage());
                throw new RuntimeException("ES bulk indexing failed", e);
            }
        }

        // ---- 索引 parent chunks ----
        List<BulkOperation> parentOps = new ArrayList<>();
        int indexedParent = 0;
        for (TextChunker.Chunk parentChunk : chunks.parentChunks()) {
            // 父 chunk 不需要再向量化（或者也可以向量化，但通常不用于检索，只作为上下文扩展）
            Map<String, Object> parentDoc = buildEsDocumentForParentChunk(parentChunk, item, globalMetadata, evidenceLevel, documentType);
            parentOps.add(BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(parentChunk.chunkId())
                            .document(parentDoc))));
            indexedParent++;
        }

        if (!parentOps.isEmpty()) {
            try {
                esClient.bulk(BulkRequest.of(b -> b.operations(parentOps)));
                log.info("Indexed {} parent chunks for doc {}", indexedParent, item.getDocId());
            } catch (Exception e) {
                log.error("Parent chunk indexing failed: {}", e.getMessage());
                // 不影响主流程，只记录
            }
        }

        // 4. Update knowledge item status
        item.setStatus(3); // indexed
        item.setChunkCount(indexedSearch);
        item.setEvidenceLevel(evidenceLevel);
        item.setDocumentType(documentType);
        itemRepository.save(item);

        return indexedSearch;
    }

    // 新增方法：构建 search chunk 的 ES 文档
    private Map<String, Object> buildEsDocumentForSearchChunk(TextChunker.Chunk chunk,
                                                            KnowledgeItem item,
                                                            MetadataExtractor.ExtractedMetadata chunkMetadata,
                                                            int evidenceLevel,
                                                            String documentType,
                                                            float[] vector) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("content", chunk.content());
        doc.put("content_vector", vector);
        doc.put("section_path", chunk.sectionPath());
        doc.put("heading_level", chunk.headingLevel());
        doc.put("parent_doc_id", item.getDocId());
        doc.put("parent_chunk_id", findParentChunkIdForSubChunk(chunk, item.getDocId())); // 根据 sectionPath 映射
        doc.put("parent_content", chunk.parentContent());  // 关键：存储父全文
        doc.put("chunk_index", chunk.chunkIndex());
        doc.put("start_char", chunk.startChar());
        doc.put("end_char", chunk.endChar());
        doc.put("document_type", documentType);
        doc.put("evidence_level", evidenceLevel);
        doc.put("publication_date", item.getPublicationDate() != null ? item.getPublicationDate().toString() : null);
        doc.put("source_name", item.getSourceName());
        doc.put("medical_entities", chunkMetadata.medicalEntities());
        doc.put("is_parent_chunk", false);
        return doc;
    }

    // 新增方法：构建 parent chunk 的 ES 文档（不包含向量，或可选包含）
    private Map<String, Object> buildEsDocumentForParentChunk(TextChunker.Chunk parentChunk,
                                                            KnowledgeItem item,
                                                            MetadataExtractor.ExtractedMetadata globalMetadata,
                                                            int evidenceLevel,
                                                            String documentType) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("content", parentChunk.content());
        // 可选：如果也想让父 chunk 可检索，可以生成向量，但通常没必要
        // doc.put("content_vector", embed(parentChunk.content()));
        doc.put("section_path", parentChunk.sectionPath());
        doc.put("heading_level", parentChunk.headingLevel());
        doc.put("parent_doc_id", item.getDocId());
        doc.put("parent_chunk_id", parentChunk.chunkId());  // 自引用
        doc.put("document_type", documentType);
        doc.put("evidence_level", evidenceLevel);
        doc.put("publication_date", item.getPublicationDate() != null ? item.getPublicationDate().toString() : null);
        doc.put("source_name", item.getSourceName());
        doc.put("medical_entities", globalMetadata.medicalEntities());
        doc.put("is_parent_chunk", true);
        return doc;
    }

    // 辅助方法：根据子块的 sectionPath 找到对应的父 chunk ID
    private String findParentChunkIdForSubChunk(TextChunker.Chunk subChunk, String parentDocId) {
        // 父 chunk ID 的生成规则：parentDocId + "_parent_" + sanitized(sectionPath)
        String sanitized = subChunk.sectionPath().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "_");
        return parentDocId + "_parent_" + sanitized;
    }

    private int resolveEvidenceLevel(KnowledgeItem item, MetadataExtractor.ExtractedMetadata metadata) {
        if (item.getEvidenceLevel() != null) return item.getEvidenceLevel();
        return 2; // default evidence level
    }

    private String resolveDocumentType(KnowledgeItem item, MetadataExtractor.ExtractedMetadata metadata) {
        if (item.getDocumentType() != null && !item.getDocumentType().isBlank()) return item.getDocumentType();
        return metadata.documentType() != null ? metadata.documentType() : "health_encyclopedia";
    }

    private float[] embed(String text) {
        try {
            var request = new EmbeddingRequest(List.of(text), null);
            var response = embeddingModel.call(request);
            if (response.getResults().isEmpty()) return null;
            return response.getResults().get(0).getOutput();
        } catch (Exception e) {
            log.error("Embedding failed: {}", e.getMessage());
            return null;
        }
    }

    public void deleteFromEs(String docId) {
        try {
            esClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("parent_doc_id").value(docId))));
            log.info("Deleted ES documents for doc {}", docId);
        } catch (Exception e) {
            log.error("Failed to delete ES documents for doc {}: {}", docId, e.getMessage());
        }
    }
}
