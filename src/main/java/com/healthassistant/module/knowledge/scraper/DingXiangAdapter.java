package com.healthassistant.module.knowledge.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DingXiangAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(DingXiangAdapter.class);

    // 已知可用的文章URL（静态HTML，可直接抓取）
    private static final List<String> KNOWN_ARTICLE_URLS = Arrays.asList(
            "https://dxy.com/article/7149",   // 血压测量
            "https://dxy.com/article/8023",   // 二甲双胍
            // 可继续添加其他已验证的URL
    );

    // 保留构造函数（不再用于动态发现，仅为依赖注入）
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
        log.info("DingXiang: using {} manually curated article URLs (JS-rendered pages skipped)", KNOWN_ARTICLE_URLS.size());
        return new ArrayList<>(KNOWN_ARTICLE_URLS);
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);

        // 提取发布日期
        String pubDate = cleaned.publishDate();
        if (pubDate == null) {
            // 优先从meta标签获取
            Element dateMeta = doc.selectFirst("meta[property=article:published_time]");
            if (dateMeta != null) {
                pubDate = dateMeta.attr("content");
            }
            if (pubDate == null || pubDate.isBlank()) {
                // 备用选择器
                Element timeEl = doc.selectFirst(".article-time, .post-time, time");
                if (timeEl != null) {
                    pubDate = timeEl.hasAttr("datetime") ? timeEl.attr("datetime")
                            : timeEl.hasAttr("content") ? timeEl.attr("content")
                            : timeEl.text().trim();
                }
            }
            if (pubDate != null && pubDate.length() >= 10) {
                pubDate = pubDate.substring(0, 10);
            }
        }

        // 确定文档类型
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

        // 证据等级：默认2，如果提及指南则提升到3
        int evidenceLevel = getDefaultEvidenceLevel();
        String bodyLower = cleaned.content().toLowerCase();
        if (bodyLower.contains("指南") || bodyLower.contains("共识") || bodyLower.contains("guideline")) {
            evidenceLevel = 3;
        }

        // 【新增】内容过短检测
        if (cleaned.content().length() < 300) {
            evidenceLevel = 1;
            log.warn("丁香医生文章 {} 内容过短 ({} 字符)，可能因动态渲染失败，证据等级降为1",
                    cleaned.url(), cleaned.content().length());
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