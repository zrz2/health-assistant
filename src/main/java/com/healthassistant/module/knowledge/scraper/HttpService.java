package com.healthassistant.module.knowledge.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

@Service
public class HttpService {

    private static final Logger log = LoggerFactory.getLogger(HttpService.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    );

    private final Map<String, Boolean> robotsCache = new ConcurrentHashMap<>();

    // 公开入口：带 robots 检查 + 重试
    public String fetch(String url) throws IOException, InterruptedException {
        if (!isAllowed(url)) {
            log.warn("Robots.txt disallows fetching: {}", url);
            throw new IOException("Access denied by robots.txt for: " + url);
        }
        return fetchRaw(url, 3);
    }

    // 内部方法：只负责重试和请求，不检查 robots（用于抓取 robots.txt 本身）
    private String fetchRaw(String url, int maxRetries) throws IOException, InterruptedException {
        int attempt = 0;
        Exception lastException = null;
        while (attempt < maxRetries) {
            try {
                String ua = USER_AGENTS.get(ThreadLocalRandom.current().nextInt(USER_AGENTS.size()));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", ua)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    log.info("Fetched {} bytes from {} (attempt {})", response.body().length(), url, attempt+1);
                    return response.body();
                } else if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    lastException = new IOException("HTTP " + response.statusCode());
                } else {
                    throw new IOException("HTTP " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                lastException = e;
            }
            attempt++;
            if (attempt < maxRetries) {
                long delay = (long) (Math.pow(2, attempt) * 1000) + ThreadLocalRandom.current().nextInt(1000);
                log.warn("Retry {} for {} after {}ms", attempt, url, delay);
                Thread.sleep(delay);
            }
        }
        throw new IOException("Failed to fetch " + url + " after " + maxRetries + " attempts", lastException);
    }

    // 安静抓取（外部使用，带 robots 检查）
    public String fetchQuietly(String url) {
        try {
            return fetch(url);
        } catch (Exception e) {
            log.warn("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    // 仅用于 robots.txt 抓取（内部使用，不递归）
    private String fetchRobotsTxt(String robotsUrl) {
        try {
            return fetchRaw(robotsUrl, 2); // 少量重试，不检查 robots
        } catch (Exception e) {
            log.debug("Failed to fetch robots.txt {}: {}", robotsUrl, e.getMessage());
            return null;
        }
    }

    private boolean isAllowed(String url) {
        String domain = extractDomain(url);
        if (domain == null) return true;
        return robotsCache.computeIfAbsent(domain, d -> {
            String robotsUrl = "https://" + d + "/robots.txt";
            log.debug("Fetching robots.txt: {}", robotsUrl);
            String robotsTxt = fetchRobotsTxt(robotsUrl); // 使用专用方法
            if (robotsTxt == null) return true;
            boolean disallowAll = robotsTxt.lines()
                    .map(String::trim)
                    .filter(line -> !line.startsWith("#"))
                    .anyMatch(line -> line.equalsIgnoreCase("Disallow: /"));
            if (disallowAll) {
                log.info("Robots.txt for {} disallows all paths", d);
                return false;
            }
            return true;
        });
    }

    private String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return null;
            return host.replaceFirst("^www\\.", "");
        } catch (Exception e) {
            log.debug("Failed to extract domain from: {}", url);
            return null;
        }
    }
}
