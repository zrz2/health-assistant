package com.healthassistant.module.knowledge.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DingXiang Doctor (丁香医生) public health articles adapter.
 * Fetches from:
 * - https://dxy.com/ (health encyclopedia)
 * - https://health.dxy.com/ (health articles)
 *
 * Note: DingXiang Doctor is a commercial platform. Only public,
 * openly accessible articles are fetched. Evidence level is lower
 * than official health authorities.
 */
@Component
public class DingXiangAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(DingXiangAdapter.class);

    private static final String DX_HEALTH_INDEX = "https://dxy.com/";
    private static final String DX_HEALTH_ARTICLE_BASE = "https://dxy.com/article/";

    // Known public health topic areas on DingXiang
    private static final String[] KNOWN_TOPIC_URLS = {
            "https://dxy.com/column/1",     // General health
            "https://dxy.com/column/2",     // Disease
            "https://dxy.com/column/3",     // Medication
            "https://dxy.com/column/4",     // Nutrition
            "https://dxy.com/column/5",     // Maternal
            "https://dxy.com/column/6",     // Chronic disease
    };

    // Known working public article URLs on DingXiang (note: site uses JS rendering,
    // most URLs need to be discovered via browser automation; these are verified working)
    private static final String[] SAMPLE_ARTICLE_URLS = {
            "https://dxy.com/article/7149",
            "https://dxy.com/article/8023",
    };

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public DingXiangAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() { return "丁香医生"; }

    @Override
    public String getDocumentType() { return "health_encyclopedia"; }

    @Override
    public int getDefaultEvidenceLevel() { return 2; }

    @Override
    public List<String> discoverUrls() {
        List<String> urls = new ArrayList<>();

        // Try column pages first
        for (String columnUrl : KNOWN_TOPIC_URLS) {
            String html = httpService.fetchQuietly(columnUrl);
            if (html != null) {
                Document doc = Jsoup.parse(html, columnUrl);
                Elements articleLinks = doc.select(
                        "a[href*='/article/'], a[href*='/know/'], .article-item a, .post-item a");
                for (Element link : articleLinks) {
                    String href = link.absUrl("href");
                    if (href.contains("dxy.com/") && !urls.contains(href)) {
                        urls.add(href);
                    }
                }
                if (!urls.isEmpty()) {
                    log.info("DingXiang: discovered {} article URLs from {}", urls.size(), columnUrl);
                    break;
                }
            }
        }

        // Fallback: use known article URLs
        if (urls.isEmpty()) {
            log.info("DingXiang: using fallback article URLs");
            for (String url : SAMPLE_ARTICLE_URLS) {
                urls.add(url);
            }
        }

        return urls.subList(0, Math.min(urls.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);

        // DingXiang often has structured metadata
        String pubDate = cleaned.publishDate();
        if (pubDate == null) {
            Element timeEl = doc.selectFirst(
                    ".article-time, .post-time, time, meta[property=article:published_time]");
            if (timeEl != null) {
                pubDate = timeEl.hasAttr("datetime") ? timeEl.attr("datetime")
                        : timeEl.hasAttr("content") ? timeEl.attr("content")
                        : timeEl.text().trim();
                if (pubDate.length() >= 10) {
                    pubDate = pubDate.substring(0, 10);
                }
            }
        }

        // Determine article category for document type
        String docType = "health_encyclopedia";
        Element category = doc.selectFirst(".article-category, .post-category, .breadcrumb");
        if (category != null) {
            String cat = category.text();
            if (cat.contains("用药") || cat.contains("药品")) {
                docType = "drug_manual";
            } else if (cat.contains("指南") || cat.contains("共识")) {
                docType = "clinical_guideline";
            } else if (cat.contains("研究") || cat.contains("论文")) {
                docType = "research_paper";
            }
        }

        // Evidence level: DingXiang is a commercial health media platform
        // Default 2, but can be higher for physician-authored content
        int evidenceLevel = getDefaultEvidenceLevel();
        String bodyLower = cleaned.content().toLowerCase();
        if (bodyLower.contains("指南") || bodyLower.contains("共识") || bodyLower.contains("guideline")) {
            evidenceLevel = 3; // References official guidelines
        }

        return new ParsedMetadata(
                cleaned.title(),
                pubDate,
                docType,
                getSourceName(),
                cleaned.url(),
                evidenceLevel
        );
    }

    @Override
    public int maxArticlesPerRun() { return 8; }
}
