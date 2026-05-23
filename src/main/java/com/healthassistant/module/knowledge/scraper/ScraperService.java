package com.healthassistant.module.knowledge.scraper;

import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full scraping pipeline:
 * Discover URLs → Fetch HTML → Clean → Parse metadata → Create knowledge item → Index to ES.
 */
@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;
    private final KnowledgeService knowledgeService;
    private final List<SourceAdapter> adapters;

    public ScraperService(HttpService httpService,
                          ContentCleaner contentCleaner,
                          KnowledgeService knowledgeService,
                          List<SourceAdapter> adapters) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
        this.knowledgeService = knowledgeService;
        this.adapters = adapters;
    }

    /**
     * Run full scraping pipeline for all registered adapters.
     * @return summary of results
     */
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

    /**
     * Scrape a single source.
     */
    public SourceReport scrapeSource(SourceAdapter adapter) {
        String sourceName = adapter.getSourceName();
        log.info("=== Scraping source: {} ===", sourceName);

        List<String> errors = new ArrayList<>();
        int discovered = 0, fetched = 0, indexed = 0;

        // 1. Discover URLs
        List<String> urls;
        try {
            urls = adapter.discoverUrls();
            discovered = urls.size();
            log.info("[{}] Discovered {} URLs", sourceName, discovered);
        } catch (Exception e) {
            return new SourceReport(sourceName, 0, 0, 0,
                    List.of("URL discovery failed: " + e.getMessage()));
        }

        if (urls.isEmpty()) {
            return new SourceReport(sourceName, 0, 0, 0,
                    List.of("No URLs discovered"));
        }

        // 2. Fetch → Clean → Parse → Index for each URL
        for (String url : urls) {
            try {
                // Fetch
                String html = httpService.fetch(url);
                if (html == null || html.isBlank()) {
                    errors.add("Empty response: " + url);
                    continue;
                }
                fetched++;

                // Clean
                ContentCleaner.CleanResult cleaned = contentCleaner.clean(html, url);
                if (cleaned.content().length() < 100) {
                    errors.add("Content too short: " + url);
                    continue;
                }

                // Parse metadata
                SourceAdapter.ParsedMetadata meta = adapter.parseMetadata(cleaned, html);

                // Parse publication date
                LocalDate pubDate = null;
                if (meta.publicationDate() != null) {
                    try {
                        pubDate = LocalDate.parse(meta.publicationDate(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (Exception e) {
                        log.debug("Could not parse date: {}", meta.publicationDate());
                    }
                }

                // Create knowledge item
                KnowledgeItem item = knowledgeService.create(
                        meta.title(),
                        cleaned.content(),
                        meta.documentType(),
                        meta.sourceName(),
                        meta.sourceUrl(),
                        pubDate,
                        meta.evidenceLevel()
                );

                // Index immediately (embed + write to ES)
                try {
                    knowledgeService.indexItem(item.getDocId());
                    indexed++;
                    log.info("[{}] Indexed: {} ({})", sourceName,
                            meta.title().substring(0, Math.min(60, meta.title().length())),
                            item.getDocId());
                } catch (Exception e) {
                    errors.add("Indexing failed for " + meta.title() + ": " + e.getMessage());
                    log.error("[{}] Indexing failed for {}: {}", sourceName, url, e.getMessage());
                }

                // Rate limiting: pause between requests to be respectful
                Thread.sleep(500);

            } catch (Exception e) {
                errors.add(url + ": " + e.getMessage());
                log.warn("[{}] Failed to process {}: {}", sourceName, url, e.getMessage());
            }
        }

        return new SourceReport(sourceName, discovered, fetched, indexed, errors);
    }

    /**
     * Scrape a single URL and return the cleaned content (for preview/debug).
     */
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
}
