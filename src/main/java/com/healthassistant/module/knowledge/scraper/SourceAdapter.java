package com.healthassistant.module.knowledge.scraper;

import java.util.List;

/**
 * Adapter interface for a health data source.
 * Each implementation handles URL discovery, HTML parsing,
 * and metadata extraction for one source (WHO, CDC, DingXiang, etc.).
 */
public interface SourceAdapter {

    /** Unique source name (e.g., "WHO", "China CDC", "丁香医生") */
    String getSourceName();

    /** Document type for this source (clinical_guideline, health_encyclopedia, etc.) */
    String getDocumentType();

    /** Default evidence level for this source (1-5) */
    int getDefaultEvidenceLevel();

    /** Discover article URLs from index/sitemap pages */
    List<String> discoverUrls() throws Exception;

    /** Parse metadata from a cleaned page for richer annotation */
    ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html);

    /** Maximum number of articles to fetch in one run */
    default int maxArticlesPerRun() { return 10; }

    record ParsedMetadata(String title, String publicationDate, String documentType,
                          String sourceName, String sourceUrl, int evidenceLevel) {}
}
