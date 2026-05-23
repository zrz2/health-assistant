package com.healthassistant.module.knowledge.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContentCleaner {

    private static final Logger log = LoggerFactory.getLogger(ContentCleaner.class);

    // Elements to remove before extracting text
    private static final String[] REMOVE_SELECTORS = {
            "script", "style", "noscript", "iframe", "svg", "canvas",
            "nav", "header", "footer", ".nav", ".header", ".footer",
            ".sidebar", ".advertisement", ".ad", ".banner", ".popup",
            ".social-share", ".comment", ".comments", ".related-posts",
            "[role=navigation]", "[role=banner]", "[role=contentinfo]",
            "form", "input", "button", "select", "textarea"
    };

    public CleanResult clean(String html, String url) {
        Document doc = Jsoup.parse(html, url);

        // Remove noisy elements
        for (String selector : REMOVE_SELECTORS) {
            Elements elements = doc.select(selector);
            for (Element el : elements) {
                el.remove();
            }
        }

        // Extract metadata from <head>
        String title = extractTitle(doc);
        String publishDate = extractPublishDate(doc);

        // Extract main content text
        String bodyText = extractMainContent(doc);

        // Clean whitespace
        bodyText = bodyText.replaceAll("\\n{3,}", "\n\n").trim();

        int originalLen = html.length();
        int cleanedLen = bodyText.length();
        log.debug("Cleaned HTML: {} -> {} chars ({:.1f}% reduction)",
                originalLen, cleanedLen, (1.0 - (double)cleanedLen / originalLen) * 100);

        return new CleanResult(title, bodyText, publishDate, url);
    }

    private String extractTitle(Document doc) {
        // Try og:title first, then <title>, then h1
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && !ogTitle.attr("content").isBlank()) {
            return ogTitle.attr("content").trim();
        }

        String title = doc.title();
        if (title != null && !title.isBlank()) {
            // Remove site name suffix like " - WHO" or " | 丁香医生"
            title = title.replaceAll("\\s*[-–|｜]\\s*.+$", "").trim();
            return title;
        }

        Element h1 = doc.selectFirst("h1");
        return h1 != null ? h1.text().trim() : "Untitled";
    }

    private String extractPublishDate(Document doc) {
        // Try common meta tags for publication date
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

    private String extractMainContent(Document doc) {
        // Try common main content containers first
        String[] contentSelectors = {
                "article", "main", "[role=main]",
                ".article-content", ".post-content", ".entry-content",
                ".content-body", ".main-content", "#content",
                ".factsheet-content", ".detail-content"
        };

        for (String sel : contentSelectors) {
            Element container = doc.selectFirst(sel);
            if (container != null) {
                String text = container.text();
                if (text.length() > 200) {
                    return text;
                }
            }
        }

        // Fallback: extract from <body> using paragraph-based approach
        Elements paragraphs = doc.select("p, li, h1, h2, h3, h4, h5, h6, blockquote, .paragraph");
        if (!paragraphs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 10 && !isNoise(text)) {
                    // Preserve heading hierarchy
                    if (p.tagName().matches("h[1-6]")) {
                        sb.append("\n").append(text).append("\n");
                    } else {
                        sb.append(text).append("\n\n");
                    }
                }
            }
            return sb.toString().trim();
        }

        // Last resort: body text
        Element body = doc.body();
        return body != null ? body.text() : "";
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
