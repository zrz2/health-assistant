package com.healthassistant.module.knowledge.scraper;

import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.entity.SyncTask;
import com.healthassistant.module.knowledge.repository.SyncTaskRepository;
import com.healthassistant.module.knowledge.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;
    private final KnowledgeService knowledgeService;
    private final SyncTaskRepository syncTaskRepository;
    private final List<SourceAdapter> adapters;

    public ScraperService(HttpService httpService,
                          ContentCleaner contentCleaner,
                          KnowledgeService knowledgeService,
                          SyncTaskRepository syncTaskRepository,
                          List<SourceAdapter> adapters) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
        this.knowledgeService = knowledgeService;
        this.syncTaskRepository = syncTaskRepository;
        this.adapters = adapters;
    
        initSiteSelectors();
    }

    private void initSiteSelectors() {
        contentCleaner.registerSiteSelectors("dxy.com", 
                ".content", ".article-content", ".post-content");
        contentCleaner.registerSiteSelectors("chinacdc.cn", 
                ".TRS_Editor", ".content", ".article-content", "#mainContent");
        contentCleaner.registerSiteSelectors("who.int", 
                ".sf-content", ".factsheet-content", ".content");
        
        // 新增站点
        contentCleaner.registerSiteSelectors("nhc.gov.cn",
                ".TRS_Editor", "#mainContent", ".content", ".article-content");
        contentCleaner.registerSiteSelectors("medjournals.cn",
                ".article-content", ".main-content", "#content");
        contentCleaner.registerSiteSelectors("medlive.cn",
                ".article-content", ".post-content", ".news-content");
        contentCleaner.registerSiteSelectors("a-hospital.com",
                "#mw-content-text", ".content", ".main-content");
    }
    
    public ScrapeReport scrapeAll() {
        ScrapeReport report = new ScrapeReport();
        for (SourceAdapter adapter : adapters) {
            try {
                SourceReport sr = scrapeSource(adapter);
                report.add(sr);
            } catch (Exception e) {
                log.error("Scraping failed for source {}: {}", adapter.getSourceName(), e.getMessage());
                report.add(new SourceReport(adapter.getSourceName(), 0, 0, 0,
                        List.of("Fatal: " + e.getMessage())));
            }
        }
        log.info("Scraping complete: {}", report);
        return report;
    }

    public SourceReport scrapeSource(SourceAdapter adapter) {
        String sourceName = adapter.getSourceName();
        log.info("=== Scraping source: {} ===", sourceName);

        // 创建同步任务记录
        SyncTask task = new SyncTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setSourceName(sourceName);
        task.setSyncType("full");
        task.setStatus(1); // 执行中
        task.setStartedAt(LocalDateTime.now());
        syncTaskRepository.save(task);

        List<String> errors = new ArrayList<>();
        int discovered = 0, fetched = 0, indexed = 0;
        int failedCount = 0;   // 专门统计失败数量（包括抓取失败、内容过短、索引失败等）

        // 1. 发现 URL
        List<String> urls;
        try {
            urls = adapter.discoverUrls();
            discovered = urls.size();
            log.info("[{}] Discovered {} URLs", sourceName, discovered);
            task.setTotalItems(discovered);
            syncTaskRepository.save(task);
        } catch (Exception e) {
            task.setStatus(3); // 失败
            task.setErrorLog("URL discovery failed: " + e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            syncTaskRepository.save(task);
            return new SourceReport(sourceName, 0, 0, 0,
                    List.of("URL discovery failed: " + e.getMessage()));
        }

        if (urls.isEmpty()) {
            task.setStatus(2); // 成功但无内容
            task.setCompletedAt(LocalDateTime.now());
            syncTaskRepository.save(task);
            return new SourceReport(sourceName, 0, 0, 0, List.of("No URLs discovered"));
        }

        // 批量去重
        Map<String, Boolean> existenceMap = knowledgeService.existsBySourceUrls(urls);
        List<String> newUrls = new ArrayList<>();
        int skipped = 0;
        for (String url : urls) {
            if (existenceMap.getOrDefault(url, false)) {
                log.info("[{}] Skipping already indexed URL: {}", sourceName, url);
                skipped++;
            } else {
                newUrls.add(url);
            }
        }

        int newUrlCount = newUrls.size();
        log.info("[{}] {} new URLs to process (skipped {} already indexed)", sourceName, newUrlCount, skipped);

        // 2. 处理每个新 URL
        for (String url : newUrls) {
            try {
                // 抓取
                String html = httpService.fetch(url);
                if (html == null || html.isBlank()) {
                    errors.add("Empty response: " + url);
                    failedCount++;
                    continue;
                }
                fetched++;

                // 清洗
                ContentCleaner.CleanResult cleaned = contentCleaner.clean(html, url);
                if (cleaned.content().length() < 100) {
                    errors.add("Content too short: " + url);
                    failedCount++;
                    continue;
                }

                // 解析元数据
                SourceAdapter.ParsedMetadata meta = adapter.parseMetadata(cleaned, html);

                // 解析发布日期
                LocalDate pubDate = null;
                if (meta.publicationDate() != null) {
                    try {
                        pubDate = LocalDate.parse(meta.publicationDate(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (Exception e) {
                        log.debug("Could not parse date: {}", meta.publicationDate());
                    }
                }

                // 创建知识条目
                KnowledgeItem item = knowledgeService.create(
                        meta.title(),
                        cleaned.content(),
                        meta.documentType(),
                        meta.sourceName(),
                        meta.sourceUrl(),
                        pubDate,
                        meta.evidenceLevel()
                );

                // 索引
                try {
                    knowledgeService.indexItem(item.getDocId());
                    indexed++;
                    log.info("[{}] Indexed: {} ({})", sourceName,
                            meta.title().substring(0, Math.min(60, meta.title().length())),
                            item.getDocId());
                } catch (Exception e) {
                    errors.add("Indexing failed for " + meta.title() + ": " + e.getMessage());
                    log.error("[{}] Indexing failed for {}: {}", sourceName, url, e.getMessage());
                    failedCount++;
                }

                // 礼貌延迟
                Thread.sleep(500);

            } catch (Exception e) {
                errors.add(url + ": " + e.getMessage());
                log.warn("[{}] Failed to process {}: {}", sourceName, url, e.getMessage());
                failedCount++;
            }
        }

        // 更新任务结果
        task.setSuccessItems(indexed);
        task.setFailedItems(failedCount);
        task.setErrorLog(String.join("\n", errors));
        task.setStatus(indexed > 0 ? 2 : 3); // 至少有一条成功则为成功，否则全失败
        task.setCompletedAt(LocalDateTime.now());
        syncTaskRepository.save(task);

        return new SourceReport(sourceName, discovered, fetched, indexed, errors);
    }

    public ContentCleaner.CleanResult previewUrl(String url) throws Exception {
        String html = httpService.fetch(url);
        return contentCleaner.clean(html, url);
    }

    public List<SourceAdapter> getAdapters() {
        return adapters;
    }

    public record SourceReport(String sourceName, int discovered, int fetched,
                                int indexed, List<String> errors) {
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    public static class ScrapeReport {
        private final List<SourceReport> sources = new ArrayList<>();
        void add(SourceReport sr) { sources.add(sr); }
        public List<SourceReport> getSources() { return sources; }
        public int totalDiscovered() { return sources.stream().mapToInt(SourceReport::discovered).sum(); }
        public int totalFetched() { return sources.stream().mapToInt(SourceReport::fetched).sum(); }
        public int totalIndexed() { return sources.stream().mapToInt(SourceReport::indexed).sum(); }
        @Override
        public String toString() {
            return String.format("Scraped %d sources: %d discovered, %d fetched, %d indexed",
                    sources.size(), totalDiscovered(), totalFetched(), totalIndexed());
        }
    }

    // 在 ScraperService 类中添加
    private boolean isMedicalContent(String content, String title) {
        if (content.length() < 200) return false;
        String lowerContent = content.toLowerCase();
        String lowerTitle = title.toLowerCase();
        // 医学关键词列表（可扩展）
        String[] medicalKeywords = {
            "糖尿病", "高血压", "疾病", "治疗", "预防", "诊断", "药物", "患者",
            "症状", "病因", "临床", "指南", "共识", "用药", "手术", "康复"
        };
        for (String kw : medicalKeywords) {
            if (lowerContent.contains(kw) || lowerTitle.contains(kw)) {
                return true;
            }
        }
        // 如果没有任何医学关键词，认为可能是非医学内容
        return false;
    }
}