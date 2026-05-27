package com.healthassistant.module.knowledge.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContentCleaner {

    private static final Logger log = LoggerFactory.getLogger(ContentCleaner.class);

    // 需要移除的无用元素选择器
    private static final String[] REMOVE_SELECTORS = {
            "script", "style", "noscript", "iframe", "svg", "canvas",
            "nav", "header", "footer", ".nav", ".header", ".footer",
            ".sidebar", ".advertisement", ".ad", ".banner", ".popup",
            ".social-share", ".comment", ".comments", ".related-posts",
            "[role=navigation]", "[role=banner]", "[role=contentinfo]",
            "form", "input", "button", "select", "textarea"
    };

    // 站点特定的内容选择器映射
    private final Map<String, String[]> siteContentSelectors = new HashMap<>();

    public void registerSiteSelectors(String domain, String... selectors) {
        siteContentSelectors.put(domain, selectors);
        log.debug("Registered content selectors for domain: {}", domain);
    }

    public CleanResult clean(String html, String url) {
        Document doc = Jsoup.parse(html, url);

        // 移除噪音元素
        for (String selector : REMOVE_SELECTORS) {
            Elements elements = doc.select(selector);
            for (Element el : elements) {
                el.remove();
            }
        }

        String title = extractTitle(doc);
        String publishDate = extractPublishDate(doc);
        String bodyText = extractMainContent(doc, url);

        // 清理多余空白
        bodyText = bodyText.replaceAll("\\n{3,}", "\n\n").trim();

        int originalLen = html.length();
        int cleanedLen = bodyText.length();
        log.debug("Cleaned HTML: {} -> {} chars ({:.1f}% reduction)",
                originalLen, cleanedLen, (1.0 - (double)cleanedLen / originalLen) * 100);

        return new CleanResult(title, bodyText, publishDate, url);
    }

    private String extractTitle(Document doc) {
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && !ogTitle.attr("content").isBlank()) {
            return ogTitle.attr("content").trim();
        }

        String title = doc.title();
        if (title != null && !title.isBlank()) {
            title = title.replaceAll("\\s*[-–|｜]\\s*.+$", "").trim();
            return title;
        }

        Element h1 = doc.selectFirst("h1");
        return h1 != null ? h1.text().trim() : "Untitled";
    }

    private String extractPublishDate(Document doc) {
        String[] selectors = {
                "meta[name=pubdate]", "meta[name=publish_date]",
                "meta[property=article:published_time]",
                "meta[name=DC.date]", "meta[name=date]",
                "time[datetime]", ".publish-date", ".pub-date",
                ".article-date", ".post-date"
        };
        for (String sel : selectors) {
            Element el = doc.selectFirst(sel);
            if (el != null) {
                String date = el.hasAttr("content") ? el.attr("content")
                        : el.hasAttr("datetime") ? el.attr("datetime")
                        : el.text().trim();
                if (!date.isBlank()) {
                    return date.substring(0, Math.min(10, date.length()));
                }
            }
        }
        return null;
    }

    private String extractMainContent(Document doc, String url) {
        String domain = extractDomain(url);
        Element container = null;

        // 1. 优先使用站点特定选择器
        if (siteContentSelectors.containsKey(domain)) {
            for (String sel : siteContentSelectors.get(domain)) {
                container = doc.selectFirst(sel);
                if (container != null && container.text().length() > 200) {
                    log.debug("Using site-specific selector '{}' for {}", sel, domain);
                    break;
                }
            }
        }

        // 2. 通用内容容器
        if (container == null) {
            String[] contentSelectors = {
                    "article", "main", "[role=main]",
                    ".article-content", ".post-content", ".entry-content",
                    ".content-body", ".main-content", "#content",
                    ".factsheet-content", ".detail-content"
            };
            for (String sel : contentSelectors) {
                container = doc.selectFirst(sel);
                if (container != null && container.text().length() > 200) {
                    break;
                }
            }
        }

        // 3. 如果找到容器，使用 preserveStructure 提取结构化的纯文本
        if (container != null) {
            return preserveStructure(container);
        }

        // 4. 后备方案：提取所有正文块（段落、标题、列表、表格）
        Elements elements = doc.select("p, li, h1, h2, h3, h4, h5, h6, blockquote, table, ul, ol");
        if (!elements.isEmpty()) {
            // 创建一个临时容器包裹这些元素，以便复用 preserveStructure
            Element wrapper = new Element("div");
            for (Element el : elements) {
                wrapper.appendChild(el.clone());
            }
            return preserveStructure(wrapper);
        }

        // 5. 最后回退：整个 body 文本
        Element body = doc.body();
        return body != null ? body.text() : "";
    }

    /**
     * 保留 HTML 结构的纯文本转换（表格→制表符分隔、列表→标记、标题→换行）
     */
    private String preserveStructure(Element container) {
        Element clone = container.clone();

        // 处理表格：转为制表符分隔的文本，保留行列结构
        Elements tables = clone.select("table");
        for (Element table : tables) {
            StringBuilder sb = new StringBuilder("\n");
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Elements cells = row.select("th,td");
                for (int i = 0; i < cells.size(); i++) {
                    sb.append(cells.get(i).text());
                    if (i < cells.size() - 1) sb.append("\t");
                }
                sb.append("\n");
            }
            // 安全替换：用纯文本块替换整个表格，避免破坏 HTML 结构
            table.text(sb.toString());
        }

        // 处理无序列表：每项前加短横线
        Elements uls = clone.select("ul");
        for (Element ul : uls) {
            for (Element li : ul.select("li")) {
                String original = li.text();
                li.text("- " + original);
            }
        }

        // 处理有序列表：每项前加数字序号
        Elements ols = clone.select("ol");
        for (Element ol : ols) {
            int idx = 1;
            for (Element li : ol.select("li")) {
                String original = li.text();
                li.text(idx++ + ". " + original);
            }
        }

        // 提取纯文本并规范化换行
        String text = clone.text();
        return text.replaceAll("\\s*\n\\s*", "\n").replaceAll("\n{2,}", "\n\n");
    }

    private String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "";
            return host.replaceAll("^www\\.", "");
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isNoise(String text) {
        String lower = text.toLowerCase();
        return lower.startsWith("cookie") || lower.startsWith("accept")
                || lower.contains("advertisement") || lower.contains("subscribe")
                || lower.contains("sign up") || lower.contains("log in")
                || lower.startsWith("share") || lower.startsWith("comments")
                || lower.contains("all rights reserved") || lower.contains("copyright")
                || lower.equals("skip to content") || lower.equals("skip to main content");
    }

    public record CleanResult(String title, String content, String publishDate, String url) {}
}
