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
public class MedLiveAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(MedLiveAdapter.class);
    private static final String BASE_URL = "https://news.medlive.cn/";
    private static final String[] CATEGORY_URLS = {
            "https://news.medlive.cn/all/",           // 全部
            "https://news.medlive.cn/endocrinology/", // 内分泌
            "https://news.medlive.cn/cardio/",        // 心血管
            "https://news.medlive.cn/neuro/"          // 神经
    };
    private static final List<String> FALLBACK_URLS = List.of(
            "https://news.medlive.cn/all/info-123456.html"
    );

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public MedLiveAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() {
        return "医脉通";
    }

    @Override
    public String getDocumentType() {
        return "health_encyclopedia";
    }

    @Override
    public int getDefaultEvidenceLevel() {
        return 3;  // 医学媒体，有一定专业性，但非官方
    }

    @Override
    public List<String> discoverUrls() throws Exception {
        List<String> urls = new ArrayList<>();
        for (String catUrl : CATEGORY_URLS) {
            String html = httpService.fetchQuietly(catUrl);
            if (html == null) continue;
            Document doc = Jsoup.parse(html, catUrl);
            // 常见文章链接选择器: .news-item a, .title a, .list a
            Elements links = doc.select("a[href*='/info-'], a[href*='/article-']");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains("medlive.cn") && !urls.contains(href)) {
                    urls.add(href);
                }
            }
            if (urls.size() >= 20) break;
        }
        if (urls.isEmpty()) {
            log.warn("MedLive: No URLs discovered, using fallback list");
            urls.addAll(FALLBACK_URLS);
        }
        return urls.subList(0, Math.min(urls.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);
        String pubDate = cleaned.publishDate();

        if (pubDate == null) {
            Element timeEl = doc.selectFirst(".time, .pub-time, .article-time");
            if (timeEl != null) {
                pubDate = timeEl.text();
                // 提取日期部分
                var matcher = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher(pubDate);
                if (matcher.find()) pubDate = matcher.group();
            }
        }

        // 医脉通的文章类型多种，可粗略分类
        String docType = "health_encyclopedia";
        if (cleaned.title().contains("指南")) {
            docType = "clinical_guideline";
        } else if (cleaned.title().contains("用药")) {
            docType = "drug_manual";
        }

        // 证据等级: 若提及指南，提高一级
        int evidenceLevel = getDefaultEvidenceLevel();
        if (cleaned.content().contains("指南") || cleaned.content().contains("共识")) {
            evidenceLevel = 4;
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
    public int maxArticlesPerRun() {
        return 20;
    }
}