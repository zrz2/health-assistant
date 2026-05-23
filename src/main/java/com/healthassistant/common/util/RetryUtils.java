package com.healthassistant.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import reactor.util.retry.Retry;

/**
 * Exponential backoff retry for DashScope API rate limiting (403/429).
 */
public final class RetryUtils {

    private static final Logger log = LoggerFactory.getLogger(RetryUtils.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 10000;

    private RetryUtils() {}

    @FunctionalInterface
    public interface RetrySupplier<T> {
        T get() throws Exception;
    }

    /**
     * Execute a synchronous call with exponential backoff retry for rate-limit errors.
     */
    public static <T> T executeWithRetry(RetrySupplier<T> supplier, String operation) throws Exception {
        Exception lastException = null;
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {}/{} for {}", attempt, MAX_RETRIES, operation);
                }
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES && isRetryable(e)) {
                    log.warn("{} failed (attempt {}): {}, retrying in {}ms",
                            operation, attempt + 1, e.getMessage(), backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                } else {
                    throw e;
                }
            }
        }
        throw lastException;
    }

    /**
     * Create a Reactor Retry spec for Flux streaming calls with exponential backoff.
     */
    public static Retry fluxRetry(String operation) {
        return Retry.backoff(MAX_RETRIES, Duration.ofMillis(INITIAL_BACKOFF_MS))
                .maxBackoff(Duration.ofMillis(MAX_BACKOFF_MS))
                .filter(RetryUtils::isRetryableError)
                .doBeforeRetry(rs -> log.warn("{} stream retry after error: {}",
                        operation, rs.failure().getMessage()));
    }

    private static boolean isRetryable(Exception e) {
        return isRetryableError(e);
    }

    private static boolean isRetryableError(Throwable t) {
        String msg = (t.getMessage() != null ? t.getMessage() : "").toLowerCase();
        return msg.contains("403") || msg.contains("429")
                || msg.contains("rate limit") || msg.contains("throttle")
                || msg.contains("too many requests") || msg.contains("too many, request");
    }
}
