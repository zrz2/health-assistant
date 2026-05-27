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
import java.util.regex.Pattern;

@Component
public class MedJournalsAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(MedJournalsAdapter.class);
    private static final String BASE_URL = "http://medjournals.cn/";
    // 部分开放获取期刊的列表页
    private static final String OA_LIST_URL = "http://medjournals.cn/oa/index.do";
    private static final List<String> FALLBACK_URLS = List.of(
            "http://medjournals.cn/oa/detail.do?docId=12345"  // 示例
    );

    private final HttpService httpService;
    private final ContentCleaner contentCleaner;

    public MedJournalsAdapter(HttpService httpService, ContentCleaner contentCleaner) {
        this.httpService = httpService;
        this.contentCleaner = contentCleaner;
    }

    @Override
    public String getSourceName() {
        return "中华医学期刊网";
    }

    @Override
    public String getDocumentType() {
        return "research_paper";
    }

    @Override
    public int getDefaultEvidenceLevel() {
        return 4;  // 学术期刊，证据等级较高
    }

    @Override
    public List<String> discoverUrls() throws Exception {
        List<String> urls = new ArrayList<>();
        String html = httpService.fetchQuietly(OA_LIST_URL);
        if (html != null) {
            Document doc = Jsoup.parse(html, OA_LIST_URL);
            // 查找文章详情链接，通常为 <a href="detail.do?docId=...">
            Elements links = doc.select("a[href*='detail.do']");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains("detail.do") && !urls.contains(href)) {
                    urls.add(href);
                }
            }
        }
        if (urls.isEmpty()) {
            log.warn("MedJournals: No URLs discovered, using fallback list");
            urls.addAll(FALLBACK_URLS);
        }
        return urls.subList(0, Math.min(urls.size(), maxArticlesPerRun()));
    }

    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        Document doc = Jsoup.parse(html);
        String pubDate = cleaned.publishDate();

        // 提取期刊文章特有的元数据：发表年份、期刊名、作者等（可选）
        Element journalEl = doc.selectFirst(".journal-name, .publication-info");
        String sourceDetail = journalEl != null ? journalEl.text() : getSourceName();

        // 发布日期通常在 meta 或页脚
        if (pubDate == null) {
            Element dateEl = doc.selectFirst("meta[name=pubdate], meta[name=date], .pub-date");
            if (dateEl != null) {
                pubDate = dateEl.hasAttr("content") ? dateEl.attr("content") : dateEl.text();
                pubDate = pubDate.length() >= 10 ? pubDate.substring(0, 10) : pubDate;
            }
        }

        // 文档类型：期刊文章
        return new ParsedMetadata(
                cleaned.title(),
                pubDate,
                "research_paper",
                getSourceName(),  // 改为固定的 "中华医学期刊网"
                cleaned.url(),
                getDefaultEvidenceLevel()
        );
    }

    @Override
    public int maxArticlesPerRun() {
        return 10;
    }
}