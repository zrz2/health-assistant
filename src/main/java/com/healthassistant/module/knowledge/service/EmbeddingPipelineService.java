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

        // 2. Extract metadata from content (medical entities, section structure)
        MetadataExtractor.ExtractedMetadata metadata = metadataExtractor.extract(
                item.getContent(), item.getTitle(), item.getSourceName());

        // Use scraper-provided evidence level and document type if available;
        // otherwise calculate from content analysis
        int evidenceLevel;
        if (item.getEvidenceLevel() != null && item.getEvidenceLevel() > 0) {
            evidenceLevel = item.getEvidenceLevel();
        } else {
            evidenceLevel = evidenceGradingService.calculateEvidenceLevel(
                    metadata.documentType(), item.getPublicationDate(), item.getSourceName());
        }

        String documentType;
        if (item.getDocumentType() != null && !item.getDocumentType().isBlank()) {
            documentType = item.getDocumentType();
        } else {
            documentType = metadata.documentType();
        }

        // 3. Embed and bulk index search chunks to ES
        int indexed = 0;
        List<BulkOperation> operations = new ArrayList<>();

        for (TextChunker.Chunk chunk : chunks.searchChunks()) {
            try {
                float[] vector = embed(chunk.content());
                if (vector == null) continue;

                Map<String, Object> doc = buildEsDocument(chunk, item, metadata, evidenceLevel, documentType, vector);
                operations.add(BulkOperation.of(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(chunk.chunkId())
                                .document(doc))));
                indexed++;
            } catch (Exception e) {
                log.error("Failed to embed chunk {}: {}", chunk.chunkId(), e.getMessage());
            }
        }

        if (!operations.isEmpty()) {
            try {
                esClient.bulk(BulkRequest.of(b -> b.operations(operations)));
                log.info("Bulk indexed {} chunks for doc {}", indexed, item.getDocId());
            } catch (Exception e) {
                log.error("Bulk indexing failed for doc {}: {}", item.getDocId(), e.getMessage());
                throw new RuntimeException("ES bulk indexing failed", e);
            }
        }

        // 4. Update knowledge item status
        item.setStatus(3); // indexed
        item.setChunkCount(indexed);
        item.setEvidenceLevel(evidenceLevel);
        item.setDocumentType(documentType);
        itemRepository.save(item);

        log.info("Knowledge item {} indexed: {} chunks, evidence level {}",
                item.getDocId(), indexed, evidenceLevel);
        return indexed;
    }

    private Map<String, Object> buildEsDocument(TextChunker.Chunk chunk, KnowledgeItem item,
                                                 MetadataExtractor.ExtractedMetadata metadata,
                                                 int evidenceLevel, String documentType, float[] vector) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("content", chunk.content());
        doc.put("content_vector", vector);
        doc.put("section_path", chunk.sectionPath());
        doc.put("heading_level", chunk.headingLevel());
        doc.put("parent_doc_id", item.getDocId());
        doc.put("document_type", documentType);
        doc.put("evidence_level", evidenceLevel);
        doc.put("publication_date", item.getPublicationDate() != null
                ? item.getPublicationDate().toString() : null);
        doc.put("source_name", item.getSourceName());
        doc.put("medical_entities", metadata.medicalEntities());
        return doc;
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
