package com.healthassistant.module.knowledge.controller;

import com.healthassistant.common.result.Result;
import com.healthassistant.module.knowledge.dto.BatchImportRequest;
import com.healthassistant.module.knowledge.dto.KnowledgeItemRequest;
import com.healthassistant.module.knowledge.dto.KnowledgeItemResponse;
import com.healthassistant.module.knowledge.dto.SyncTaskResponse;
import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.entity.SyncTask;
import com.healthassistant.module.knowledge.repository.SyncTaskRepository;
import com.healthassistant.module.knowledge.scraper.ContentCleaner;
import com.healthassistant.module.knowledge.scraper.ScraperService;
import com.healthassistant.module.knowledge.service.ImportService;
import com.healthassistant.module.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "知识库管理")
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final ImportService importService;
    private final SyncTaskRepository syncTaskRepository;
    private final ScraperService scraperService;

    public KnowledgeController(KnowledgeService knowledgeService, ImportService importService,
                               SyncTaskRepository syncTaskRepository,
                               ScraperService scraperService) {
        this.knowledgeService = knowledgeService;
        this.importService = importService;
        this.syncTaskRepository = syncTaskRepository;
        this.scraperService = scraperService;
    }

    @Operation(summary = "创建知识条目")
    @PostMapping("/items")
    public Result<KnowledgeItemResponse> createItem(@Valid @RequestBody KnowledgeItemRequest request) {
        KnowledgeItem item = knowledgeService.create(
                request.getTitle(), request.getContent(),
                request.getDocumentType(), request.getSourceName(),
                request.getSourceUrl(), request.getPublicationDate());
        return Result.success("知识条目创建成功", KnowledgeItemResponse.from(item));
    }

    @Operation(summary = "索引知识条目（切分+向量化+写入ES）")
    @PostMapping("/items/{docId}/index")
    public Result<KnowledgeItemResponse> indexItem(@PathVariable String docId) {
        KnowledgeItem item = knowledgeService.indexItem(docId);
        return Result.success("索引成功", KnowledgeItemResponse.from(item));
    }

    @Operation(summary = "获取知识条目详情")
    @GetMapping("/items/{docId}")
    public Result<KnowledgeItemResponse> getItem(@PathVariable String docId) {
        return Result.success(KnowledgeItemResponse.from(knowledgeService.getByDocId(docId)));
    }

    @Operation(summary = "分页查询知识条目")
    @GetMapping("/items")
    public Result<Page<KnowledgeItemResponse>> listItems(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeItem> items = knowledgeService.listByStatus(status, pageable);
        return Result.success(items.map(KnowledgeItemResponse::from));
    }

    @Operation(summary = "删除知识条目")
    @DeleteMapping("/items/{docId}")
    public Result<Void> deleteItem(@PathVariable String docId) {
        knowledgeService.deleteItem(docId);
        return Result.success();
    }

    @Operation(summary = "重新索引全部知识条目")
    @PostMapping("/reindex")
    public Result<Integer> reindexAll() {
        int count = knowledgeService.reindexAll();
        return Result.success("重建索引完成，共处理 " + count + " 条", count);
    }

    @Operation(summary = "导入单个文档（创建+索引）")
    @PostMapping("/import/single")
    public Result<ImportService.ImportResult> importSingle(@Valid @RequestBody KnowledgeItemRequest request) {
        ImportService.ImportResult result = importService.importDocument(
                request.getTitle(), request.getContent(),
                request.getDocumentType(), request.getSourceName(),
                request.getSourceUrl(), request.getPublicationDate());
        return Result.success("导入成功", result);
    }

    @Operation(summary = "批量导入文档")
    @PostMapping("/import/batch")
    public Result<SyncTaskResponse> batchImport(@Valid @RequestBody BatchImportRequest request) {
        List<ImportService.DocumentInput> docs = request.getDocuments().stream()
                .map(d -> new ImportService.DocumentInput(
                        d.getTitle(), d.getContent(), d.getUrl(), d.getPublicationDate()))
                .toList();
        SyncTask task = importService.batchImport(request.getSourceName(), docs);
        return Result.success("批量导入已启动", SyncTaskResponse.from(task));
    }

    @Operation(summary = "获取同步任务状态")
    @GetMapping("/sync-tasks/{taskId}")
    public Result<SyncTaskResponse> getSyncTask(@PathVariable String taskId) {
        SyncTask task = syncTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("同步任务不存在: " + taskId));
        return Result.success(SyncTaskResponse.from(task));
    }

    @Operation(summary = "启动数据抓取（WHO/CDC/DingXiang），自动发现、清洗、索引健康知识")
    @PostMapping("/scrape")
    public Result<String> startScraping() {
        ScraperService.ScrapeReport report = scraperService.scrapeAll();
        return Result.success(String.format(
                "抓取完成: %d 来源, 发现 %d 篇, 获取 %d 篇, 索引 %d 篇",
                report.getSources().size(), report.totalDiscovered(),
                report.totalFetched(), report.totalIndexed()));
    }

    @Operation(summary = "预览某个URL的清洗结果（调试用）")
    @PostMapping("/scrape/preview")
    public Result<String> previewUrl(@RequestParam String url) {
        try {
            ContentCleaner.CleanResult result = scraperService.previewUrl(url);
            return Result.success("标题: " + result.title() + "\n\n" + result.content());
        } catch (Exception e) {
            return Result.error(9002, "抓取失败: " + e.getMessage());
        }
    }
}
