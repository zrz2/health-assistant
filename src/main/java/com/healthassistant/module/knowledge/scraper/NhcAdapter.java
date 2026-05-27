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
import java.util.regex.Pattern;

/**
 * 国家卫生健康委员会官网适配器
 * 来源: http://www.nhc.gov.cn/
 * 抓取政策文件、通知公告、指南等
 */
@Component
public class NhcAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(NhcAdapter.class);

    private static final String BASE_URL = "http://www.nhc.gov.cn/";
    private static final String[] TARGET_SECTIONS = {
            "wjw/xxgk/policy",      // 政策文件
            "wjw/xxgk/gzdt",        // 工作动态
            "wjw/xxgk/znzz",        // 指南
            "wjw/xxgk/tzgg"         // 通知公告
    };
    // 已知的稳定URL (备用)
    private static final List<String> FALLBACK_URLS = List.of(
            "http://www.nhc.gov.cn/wjw/xxgk/policy/202405/abc123.html"
            // 实际应填入已验证的URL
    );

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public NhcAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() {
        return "国家卫健委";
    }

    @Override
    public String getDocumentType() {
        return "clinical_guideline";  // 卫健委文件多为指南或政策
    }

    @Override
    public int getDefaultEvidenceLevel() {
        return 5;  // 国家级官方机构，最高证据等级
    }

    @Override
    public List<String> discoverUrls() throws Exception {
        List<String> urls = new ArrayList<>();

        for (String section : TARGET_SECTIONS) {
            String listUrl = BASE_URL + section;
            String html = httpService.fetchQuietly(listUrl);
            if (html == null) {
                log.warn("Failed to fetch list page: {}", listUrl);
                continue;
            }
            Document doc = Jsoup.parse(html, listUrl);
            // 常见列表页链接选择器: .list a, .news-list a, .list-content a
            Elements links = doc.select("a[href*='.html'], a[href*='.htm']");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains("nhc.gov.cn") && !urls.contains(href)) {
                    urls.add(href);
                }
            }
            // 限制每节最多抓取20条，避免过多
            if (urls.size() >= 20) break;
        }

        // 去重并限制数量
        List<String> distinct = urls.stream().distinct().toList();
        if (distinct.isEmpty()) {
            log.warn("NHC: No URLs discovered, using fallback list");
            distinct = new ArrayList<>(FALLBACK_URLS);
        }
        return distinct.subList(0, Math.min(distinct.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);
        String pubDate = cleaned.publishDate();

        // 尝试从页面中提取发布日期
        if (pubDate == null) {
            Element dateEl = doc.selectFirst(".info-date, .pub-date, .time, .article-time");
            if (dateEl != null) {
                String raw = dateEl.text();
                // 匹配 "2024-03-15" 或 "2024年03月15日"
                var matcher = Pattern.compile("\\d{4}[-年]\\d{1,2}[-月]\\d{1,2}").matcher(raw);
                if (matcher.find()) {
                    pubDate = matcher.group().replaceAll("[年]", "-").replaceAll("[月]", "");
                }
            }
        }

        // 文档类型：根据URL或标题判断
        String url = cleaned.url();
        String docType = "clinical_guideline";
        if (url.contains("tzgg") || url.contains("gzt")) {
            docType = "health_encyclopedia";  // 通知公告类
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
    public int maxArticlesPerRun() {
        return 15;
    }
}