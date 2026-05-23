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
 * WHO (World Health Organization) fact sheets and health topics adapter.
 * Fetches from:
 * - https://www.who.int/news-room/fact-sheets
 * - https://www.who.int/health-topics
 */
@Component
public class WhoAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhoAdapter.class);

    private static final String FACT_SHEET_INDEX =
            "https://www.who.int/news-room/fact-sheets";
    private static final String HEALTH_TOPICS =
            "https://www.who.int/health-topics";

    // High-priority health topics relevant to the assistant
    private static final String[] PRIORITY_TOPICS = {
            "hypertension", "diabetes", "cardiovascular-diseases",
            "cancer", "mental-health", "nutrition",
            "physical-activity", "tobacco", "obesity",
            "immunization", "tuberculosis", "hiv-aids"
    };

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public WhoAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() { return "WHO"; }

    @Override
    public String getDocumentType() { return "clinical_guideline"; }

    @Override
    public int getDefaultEvidenceLevel() { return 5; }

    @Override
    public List<String> discoverUrls() {
        List<String> urls = new ArrayList<>();

        // Try to get fact sheet listing
        String indexHtml = httpService.fetchQuietly(FACT_SHEET_INDEX);
        if (indexHtml != null) {
            Document doc = Jsoup.parse(indexHtml, FACT_SHEET_INDEX);
            Elements links = doc.select("a[href*='/fact-sheets/detail/']");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (!href.isBlank()) {
                    urls.add(href);
                }
            }
            log.info("WHO: discovered {} fact sheet URLs", urls.size());
        }

        // If index page fails or is empty, use known fact sheet URLs
        if (urls.isEmpty()) {
            log.info("WHO: using fallback fact sheet URLs");
            for (String topic : PRIORITY_TOPICS) {
                urls.add("https://www.who.int/news-room/fact-sheets/detail/" + topic);
            }
        }

        return urls.subList(0, Math.min(urls.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);

        // Try to extract more precise publication date
        String pubDate = cleaned.publishDate();
        if (pubDate == null) {
            Element dateEl = doc.selectFirst("meta[name=DC.date], .factsheet-date, .date");
            if (dateEl != null) {
                pubDate = dateEl.hasAttr("content") ? dateEl.attr("content") : dateEl.text();
            }
        }

        // Detect document subtype from URL (more reliable than page content)
        String url = cleaned.url();
        String docType = "clinical_guideline";
        if (url.contains("fact-sheets")) {
            docType = "clinical_guideline";
        } else if (url.contains("health-topics")) {
            docType = "health_encyclopedia";
        }

        return new ParsedMetadata(
                cleaned.title(),
                pubDate != null ? pubDate.substring(0, Math.min(10, pubDate.length())) : null,
                docType,
                getSourceName(),
                cleaned.url(),
                getDefaultEvidenceLevel()
        );
    }

    @Override
    public int maxArticlesPerRun() { return 10; }
}
