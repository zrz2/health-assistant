package com.healthassistant.common.health;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchHealthIndicator.class);

    private final ElasticsearchClient esClient;

    public ElasticsearchHealthIndicator(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public Health health() {
        try {
            var response = esClient.cluster().health();
            HealthStatus status = response.status();
            int nodeCount = response.numberOfNodes();

            if (status == HealthStatus.Green || status == HealthStatus.Yellow) {
                return Health.up()
                        .withDetail("status", status.jsonValue())
                        .withDetail("nodes", nodeCount)
                        .build();
            }
            return Health.down()
                    .withDetail("status", status.jsonValue())
                    .withDetail("nodes", nodeCount)
                    .build();
        } catch (Exception e) {
            log.warn("Elasticsearch health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
