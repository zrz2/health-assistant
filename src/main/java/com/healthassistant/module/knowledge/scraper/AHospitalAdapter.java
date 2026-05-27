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

@Component
public class AHospitalAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(AHospitalAdapter.class);
    private static final String BASE_URL = "http://www.a-hospital.com/";
    private static final String INDEX_URL = "http://www.a-hospital.com/w/";
    private static final List<String> FALLBACK_URLS = List.of(
            "http://www.a-hospital.com/w/%E7%B3%96%E5%B0%BF%E7%97%85",
            "http://www.a-hospital.com/w/%E9%AB%98%E8%A1%80%E5%8E%8B"
    );

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public AHospitalAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() {
        return "A+医学百科";
    }

    @Override
    public String getDocumentType() {
        return "health_encyclopedia";
    }

    @Override
    public int getDefaultEvidenceLevel() {
        return 2;  // 类似维基，权威性中等
    }

    @Override
    public List<String> discoverUrls() throws Exception {
        List<String> urls = new ArrayList<>();
        String html = httpService.fetchQuietly(INDEX_URL);
        if (html != null) {
            Document doc = Jsoup.parse(html, INDEX_URL);
            // 百科页面通常以 /w/ 开头
            Elements links = doc.select("a[href^='/w/']");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains(BASE_URL) && !urls.contains(href)) {
                    urls.add(href);
                }
            }
        }
        if (urls.isEmpty()) {
            log.warn("AHospital: No URLs discovered, using fallback list");
            urls.addAll(FALLBACK_URLS);
        }
        return urls.subList(0, Math.min(urls.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        // 百科类页面通常没有明确发布日期，默认当前日期或留空
        String pubDate = cleaned.publishDate();
        if (pubDate == null) {
            // 尝试抓取编辑日期 (页面底部可能有)
            Document doc = Jsoup.parse(html);
            Element footer = doc.selectFirst(".last-modified, .update-time");
            if (footer != null) {
                String text = footer.text();
                var matcher = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher(text);
                if (matcher.find()) pubDate = matcher.group();
            }
        }

        // 文档类型：百科默认为 encyclopedia
        return new ParsedMetadata(
                cleaned.title(),
                pubDate,
                "health_encyclopedia",
                getSourceName(),
                cleaned.url(),
                getDefaultEvidenceLevel()
        );
    }

    @Override
    public int maxArticlesPerRun() {
        return 30;
    }
}