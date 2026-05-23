package com.healthassistant.module.knowledge.service;

import com.healthassistant.common.util.IdGenerator;
import com.healthassistant.module.knowledge.entity.KnowledgeSource;
import com.healthassistant.module.knowledge.entity.SyncTask;
import com.healthassistant.module.knowledge.repository.KnowledgeSourceRepository;
import com.healthassistant.module.knowledge.repository.SyncTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final KnowledgeService knowledgeService;
    private final KnowledgeSourceRepository sourceRepository;
    private final SyncTaskRepository syncTaskRepository;

    public ImportService(KnowledgeService knowledgeService,
                         KnowledgeSourceRepository sourceRepository,
                         SyncTaskRepository syncTaskRepository) {
        this.knowledgeService = knowledgeService;
        this.sourceRepository = sourceRepository;
        this.syncTaskRepository = syncTaskRepository;
    }

    /**
     * Import a single document and index it immediately.
     */
    @Transactional
    public ImportResult importDocument(String title, String content, String documentType,
                                        String sourceName, String sourceUrl,
                                        LocalDate publicationDate) {
        String docId = IdGenerator.generateDocId();
        knowledgeService.create(title, content, documentType, sourceName, sourceUrl, publicationDate);
        knowledgeService.indexItem(docId);
        return new ImportResult(docId, title, 1);
    }

    /**
     * Batch import multiple documents.
     */
    @Transactional
    public SyncTask batchImport(String sourceName, java.util.List<DocumentInput> documents) {
        KnowledgeSource source = sourceRepository.findByName(sourceName)
                .orElseThrow(() -> new RuntimeException("Knowledge source not found: " + sourceName));

        SyncTask task = new SyncTask();
        task.setTaskId(IdGenerator.generateTaskId());
        task.setSourceName(sourceName);
        task.setSyncType("full");
        task.setTotalItems(documents.size());
        task.setStatus(1); // executing
        task.setStartedAt(LocalDateTime.now());
        task = syncTaskRepository.save(task);

        int success = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();

        for (DocumentInput input : documents) {
            try {
                knowledgeService.create(input.title(), input.content(),
                        source.getSourceType(), sourceName,
                        input.url(), input.publicationDate());
                success++;
            } catch (Exception e) {
                failed++;
                errors.append(input.title()).append(": ").append(e.getMessage()).append("\n");
                log.error("Import failed for {}: {}", input.title(), e.getMessage());
            }
        }

        task.setSuccessItems(success);
        task.setFailedItems(failed);
        task.setStatus(failed == documents.size() ? 3 : (failed > 0 ? 2 : 2));
        task.setErrorLog(errors.isEmpty() ? null : errors.toString());
        task.setCompletedAt(LocalDateTime.now());
        syncTaskRepository.save(task);

        log.info("Batch import completed: {}/{} success", success, documents.size());
        return task;
    }

    public record DocumentInput(String title, String content, String url,
                                 LocalDate publicationDate) {}

    public record ImportResult(String docId, String title, int chunkCount) {}
}
