package com.healthassistant.module.knowledge.service;

import com.healthassistant.common.util.IdGenerator;
import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.repository.KnowledgeItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;


@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeItemRepository itemRepository;
    private final EmbeddingPipelineService pipelineService;

    public KnowledgeService(KnowledgeItemRepository itemRepository,
                            EmbeddingPipelineService pipelineService) {
        this.itemRepository = itemRepository;
        this.pipelineService = pipelineService;
    }

    @Transactional
    public KnowledgeItem create(String title, String content, String documentType,
                                 String sourceName, String sourceUrl, LocalDate publicationDate) {
        return create(title, content, documentType, sourceName, sourceUrl, publicationDate, null);
    }

    @Transactional
    public KnowledgeItem create(String title, String content, String documentType,
                                 String sourceName, String sourceUrl, LocalDate publicationDate,
                                 Integer evidenceLevel) {
        KnowledgeItem item = new KnowledgeItem();
        item.setDocId(IdGenerator.generateDocId());
        item.setTitle(title);
        item.setContent(content);
        item.setDocumentType(documentType);
        item.setSourceName(sourceName);
        item.setSourceUrl(sourceUrl);
        item.setPublicationDate(publicationDate);
        item.setEvidenceLevel(evidenceLevel);
        item.setStatus(0); // pending
        item = itemRepository.save(item);

        log.info("Created knowledge item: {} - {}", item.getDocId(), title);
        return item;
    }

    @Transactional
    public KnowledgeItem indexItem(String docId) {
        KnowledgeItem item = itemRepository.findByDocId(docId)
                .orElseThrow(() -> new RuntimeException("Knowledge item not found: " + docId));

        item.setStatus(1); // chunking in progress
        itemRepository.save(item);

        try {
            pipelineService.indexKnowledgeItem(item);
        } catch (Exception e) {
            log.error("Indexing failed for {}: {}", docId, e.getMessage());
            item.setStatus(4); // error - need to add this status
            itemRepository.save(item);
            throw new RuntimeException("Indexing failed", e);
        }

        return itemRepository.findByDocId(docId).orElseThrow();
    }

    @Transactional
    public void deleteItem(String docId) {
        KnowledgeItem item = itemRepository.findByDocId(docId)
                .orElseThrow(() -> new RuntimeException("Knowledge item not found: " + docId));

        pipelineService.deleteFromEs(docId);
        itemRepository.delete(item);
        log.info("Deleted knowledge item: {}", docId);
    }

    @Transactional
    public int reindexAll() {
        List<KnowledgeItem> items = itemRepository.findByStatusOrderByCreatedAtAsc(3); // indexed
        int count = 0;
        for (KnowledgeItem item : items) {
            try {
                pipelineService.deleteFromEs(item.getDocId());
                pipelineService.indexKnowledgeItem(item);
                count++;
            } catch (Exception e) {
                log.error("Reindex failed for {}: {}", item.getDocId(), e.getMessage());
            }
        }
        log.info("Reindexed {} knowledge items", count);
        return count;
    }

    public Map<String, Boolean> existsBySourceUrls(List<String> sourceUrls) {
        List<String> existingUrls = itemRepository.findExistingSourceUrls(sourceUrls);
        Map<String, Boolean> result = new HashMap<>();
        if (sourceUrls == null || sourceUrls.isEmpty()) {
            return new HashMap<>();
        }
        for (String url : sourceUrls) {
            result.put(url, existingUrls.contains(url));
        }
        return result;
    }

    public KnowledgeItem getByDocId(String docId) {
        return itemRepository.findByDocId(docId)
                .orElseThrow(() -> new RuntimeException("Knowledge item not found: " + docId));
    }

    public Page<KnowledgeItem> listByStatus(Integer status, Pageable pageable) {
        if (status != null) {
            return itemRepository.findByStatus(status, pageable);
        }
        return itemRepository.findAll(pageable);
    }

    public List<KnowledgeItem> getBySource(String sourceName) {
        return itemRepository.findBySourceName(sourceName);
    }

    public long countByStatus(Integer status) {
        return itemRepository.countByStatus(status);
    }

    public boolean existsBySourceUrl(String sourceUrl) {
        return itemRepository.findBySourceUrl(sourceUrl).isPresent();
    }
}
