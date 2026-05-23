package com.healthassistant.module.knowledge.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Standalone scraper runner.
 * Activate with Spring profile "scraper" to auto-run on startup.
 *
 * Usage:
 *   java -jar health-assistant.jar --spring.profiles.active=dev,scraper
 *   or
 *   mvn spring-boot:run -Dspring-boot.run.profiles=dev,scraper
 *
 * The application will start, run all configured source adapters,
 * import and index the health knowledge, then exit.
 */
@Component
@Profile("scraper")
public class ScraperRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ScraperRunner.class);

    private final ScraperService scraperService;

    public ScraperRunner(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public void run(String... args) {
        log.info("==========================================");
        log.info("  Health Knowledge Scraper - Starting");
        log.info("  Sources: {} adapters registered", scraperService.getAdapters().size());
        log.info("==========================================");

        ScraperService.ScrapeReport report = scraperService.scrapeAll();

        log.info("==========================================");
        log.info("  Scraping Complete");
        log.info("  Total discovered: {}", report.totalDiscovered());
        log.info("  Total fetched:    {}", report.totalFetched());
        log.info("  Total indexed:    {}", report.totalIndexed());
        log.info("==========================================");

        for (ScraperService.SourceReport sr : report.getSources()) {
            log.info("[{}] discovered={} fetched={} indexed={} errors={}",
                    sr.sourceName(), sr.discovered(), sr.fetched(),
                    sr.indexed(), sr.errors().size());
            if (sr.hasErrors()) {
                for (String err : sr.errors()) {
                    log.warn("  - {}", err);
                }
            }
        }

        log.info("Scraper run finished. Exiting in 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Done.");
    }
}
