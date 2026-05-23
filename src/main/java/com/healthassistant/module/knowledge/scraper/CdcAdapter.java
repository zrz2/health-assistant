package com.healthassistant.module.knowledge.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * China CDC (中国疾控中心) health guidelines and fact sheets adapter.
 * Fetches from:
 * - https://www.chinacdc.cn/jkzt/ (health topics)
 * - https://www.chinacdc.cn/yyrdgz/ (hot topics)
 */
@Component
public class CdcAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(CdcAdapter.class);

    // China CDC has restructured; current health topic pages
    private static final String CDC_MAIN = "https://www.chinacdc.cn/";
    private static final String NCNCD_NEWS = "https://ncncd.chinacdc.cn/zxdt/";

    // Known working article URLs from China CDC (2025-2026)
    private static final String[] KNOWN_ARTICLE_URLS = {
            "https://www.chinacdc.cn/zxyw/202512/t20251202_313809.html",
            "https://ncncd.chinacdc.cn/zxdt/202512/t20251201_313921.htm",
            "https://ncncd.chinacdc.cn/zxdt/202509/t20250922_313888.htm",
            "https://ncncd.chinacdc.cn/zxdt/202511/t20251128_313915.htm",
            "https://ncncd.chinacdc.cn/zxdt/202605/t20260519_1835912.htm",
    };

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public CdcAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() { return "中国疾控中心"; }

    @Override
    public String getDocumentType() { return "clinical_guideline"; }

    @Override
    public int getDefaultEvidenceLevel() { return 4; }

    @Override
    public List<String> discoverUrls() {
        List<String> urls = new ArrayList<>();

        // Try to discover articles from CDC main page
        String html = httpService.fetchQuietly(CDC_MAIN);
        if (html != null) {
            Document doc = Jsoup.parse(html, CDC_MAIN);
            Elements links = doc.select(
                    "a[href*='.html'], a[href*='.htm'], .list-box a, .news-list a, .article-list a");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains("chinacdc.cn") && !href.equals(CDC_MAIN)) {
                    urls.add(href);
                }
            }
            log.info("CDC: discovered {} URLs from main page", urls.size());
        }

        // Also try chronic disease center news page
        String ncncdHtml = httpService.fetchQuietly(NCNCD_NEWS);
        if (ncncdHtml != null) {
            Document doc = Jsoup.parse(ncncdHtml, NCNCD_NEWS);
            Elements links = doc.select(
                    "a[href*='.html'], a[href*='.htm'], .list-box a, .news-list a");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains("chinacdc.cn") && !urls.contains(href)) {
                    urls.add(href);
                }
            }
            log.info("CDC: discovered {} URLs from NCNCD page", urls.size());
        }

        // Fallback: use known working article URLs
        if (urls.isEmpty()) {
            log.info("CDC: using known article URLs as fallback");
            for (String url : KNOWN_ARTICLE_URLS) {
                urls.add(url);
            }
        }

        return urls.subList(0, Math.min(urls.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);

        String pubDate = cleaned.publishDate();
        if (pubDate == null) {
            // China CDC common date patterns
            Element dateEl = doc.selectFirst(
                    ".info-source, .article-date, .pub-date, .time, meta[name=pubdate]");
            if (dateEl != null) {
                String text = dateEl.hasAttr("content") ? dateEl.attr("content") : dateEl.text();
                // Parse Chinese date formats like "2024-03-15" or "2024年03月15日"
                text = text.replaceAll("[年月]", "-").replaceAll("[日号]", "").trim();
                pubDate = text.length() >= 10 ? text.substring(0, 10) : text;
                // Validate
                try {
                    LocalDate.parse(pubDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    pubDate = null;
                }
            }
        }

        // Detect document type from breadcrumb or category
        String docType = "clinical_guideline";
        Element category = doc.selectFirst(".breadcrumb, .position, .category");
        if (category != null) {
            String catText = category.text();
            if (catText.contains("新闻") || catText.contains("动态")) {
                docType = "health_encyclopedia";
            }
        }

        return new ParsedMetadata(
                cleaned.title(),
                pubDate,
                docType,
                getSourceName(),
                cleaned.url(),
                getDefaultEvidenceLevel()
        );
    }

    @Override
    public int maxArticlesPerRun() { return 10; }
}
