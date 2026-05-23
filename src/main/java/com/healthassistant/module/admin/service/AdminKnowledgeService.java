package com.healthassistant.module.admin.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.common.util.IdGenerator;
import com.healthassistant.module.admin.dto.KnowledgeImportRequest;
import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.entity.SyncTask;
import com.healthassistant.module.knowledge.repository.KnowledgeItemRepository;
import com.healthassistant.module.knowledge.repository.SyncTaskRepository;
import com.healthassistant.module.knowledge.scraper.ScraperService;
import com.healthassistant.module.knowledge.service.EmbeddingPipelineService;
import com.healthassistant.module.knowledge.service.KnowledgeService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(AdminKnowledgeService.class);

    private final KnowledgeItemRepository itemRepository;
    private final SyncTaskRepository syncTaskRepository;
    private final KnowledgeService knowledgeService;
    private final EmbeddingPipelineService pipelineService;
    private final ScraperService scraperService;

    public AdminKnowledgeService(KnowledgeItemRepository itemRepository,
                                  SyncTaskRepository syncTaskRepository,
                                  KnowledgeService knowledgeService,
                                  EmbeddingPipelineService pipelineService,
                                  ScraperService scraperService) {
        this.itemRepository = itemRepository;
        this.syncTaskRepository = syncTaskRepository;
        this.knowledgeService = knowledgeService;
        this.pipelineService = pipelineService;
        this.scraperService = scraperService;
    }

    public Page<KnowledgeItem> listItems(String keyword, String sourceName,
                                          String documentType, Integer status, Pageable pageable) {
        Specification<KnowledgeItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("content"), pattern)
                ));
            }
            if (sourceName != null && !sourceName.isBlank()) {
                predicates.add(cb.equal(root.get("sourceName"), sourceName));
            }
            if (documentType != null && !documentType.isBlank()) {
                predicates.add(cb.equal(root.get("documentType"), documentType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return itemRepository.findAll(spec, pageable);
    }

    public KnowledgeItem getItem(String docId) {
        return itemRepository.findByDocId(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND, "知识条目不存在"));
    }

    @Transactional
    public KnowledgeItem importSingle(KnowledgeImportRequest request) {
        LocalDate pubDate = null;
        if (request.getPublicationDate() != null && !request.getPublicationDate().isBlank()) {
            try {
                pubDate = LocalDate.parse(request.getPublicationDate());
            } catch (Exception e) {
                log.debug("Could not parse date: {}", request.getPublicationDate());
            }
        }
        KnowledgeItem item = knowledgeService.create(
                request.getTitle(),
                request.getContent(),
                request.getDocumentType() != null ? request.getDocumentType() : "health_encyclopedia",
                request.getSourceName(),
                request.getSourceUrl(),
                pubDate,
                request.getEvidenceLevel()
        );
        knowledgeService.indexItem(item.getDocId());
        return item;
    }

    @Async
    @Transactional
    public void importBatch(KnowledgeImportRequest.BatchImportRequest request) {
        String sourceName = request.getSourceName();
        String docType = request.getDocumentType() != null ? request.getDocumentType() : "health_encyclopedia";

        SyncTask task = new SyncTask();
        task.setTaskId(IdGenerator.generateTaskId());
        task.setSourceName(sourceName);
        task.setSyncType("full");
        task.setTotalItems(request.getArticles().size());
        task.setStatus(1);
        task.setStartedAt(LocalDateTime.now());
        task = syncTaskRepository.save(task);

        int success = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();

        for (KnowledgeImportRequest.Article article : request.getArticles()) {
            try {
                LocalDate pubDate = null;
                if (article.getPublicationDate() != null) {
                    try {
                        pubDate = LocalDate.parse(article.getPublicationDate());
                    } catch (Exception ignored) {}
                }
                KnowledgeItem item = knowledgeService.create(
                        article.getTitle(), article.getContent(), docType,
                        sourceName, article.getSourceUrl(), pubDate, null);
                knowledgeService.indexItem(item.getDocId());
                success++;
            } catch (Exception e) {
                failed++;
                errors.append(article.getTitle()).append(": ").append(e.getMessage()).append("\n");
                log.error("Batch import failed for {}: {}", article.getTitle(), e.getMessage());
            }
        }

        task.setSuccessItems(success);
        task.setFailedItems(failed);
        task.setStatus(failed == request.getArticles().size() ? 3 : 2);
        task.setErrorLog(errors.isEmpty() ? null : errors.toString());
        task.setCompletedAt(LocalDateTime.now());
        syncTaskRepository.save(task);

        log.info("Batch import: {}/{} success", success, request.getArticles().size());
    }

    @Transactional
    public void deleteItem(String docId) {
        knowledgeService.deleteItem(docId);
    }

    @Transactional
    public void batchDelete(List<String> docIds) {
        for (String docId : docIds) {
            try {
                knowledgeService.deleteItem(docId);
            } catch (Exception e) {
                log.error("Batch delete failed for {}: {}", docId, e.getMessage());
            }
        }
    }

    @Transactional
    public int reindexAll() {
        return knowledgeService.reindexAll();
    }

    @Transactional
    public void reindexItem(String docId) {
        KnowledgeItem item = itemRepository.findByDocId(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND, "知识条目不存在"));
        pipelineService.deleteFromEs(docId);
        pipelineService.indexKnowledgeItem(item);
    }

    public ScraperService.ScrapeReport triggerScrape() {
        return scraperService.scrapeAll();
    }

    public ScraperService.SourceReport triggerScrapeSource(String sourceName) {
        return scraperService.getAdapters().stream()
                .filter(a -> a.getSourceName().equals(sourceName))
                .findFirst()
                .map(scraperService::scrapeSource)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND,
                        "未找到来源适配器: " + sourceName));
    }

    public Page<SyncTask> getImportHistory(Pageable pageable) {
        return syncTaskRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public ScraperService.ScrapeReport getScrapeReport() {
        // Returns last scrape report by triggering - for production, cache this
        return scraperService.scrapeAll();
    }
}
