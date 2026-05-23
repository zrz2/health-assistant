package com.healthassistant.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PostConstruct;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.StringReader;

@Configuration
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:health_knowledge}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1024}")
    private int dimensions;

    private ElasticsearchClient esClient;

    @Bean
    public RestClient restClient() {
        var builder = RestClient.builder(HttpHost.create(uris));

        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            var cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(hc ->
                    hc.setDefaultCredentialsProvider(cp));
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);
        return esClient;
    }

    /**
     * Auto-create the ES index with proper mapping on startup if it doesn't exist.
     */
    @PostConstruct
    public void initializeIndex() {
        try {
            // Wait for ES connection to be ready, then check/init index
            new Thread(() -> {
                for (int i = 0; i < 30; i++) {
                    try {
                        if (esClient != null) {
                            boolean exists = esClient.indices()
                                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                                    .value();
                            if (!exists) {
                                createIndex();
                            } else {
                                log.info("ES index '{}' already exists", indexName);
                            }
                            return;
                        }
                    } catch (Exception e) {
                        log.debug("Waiting for ES... ({}/30)", i + 1);
                    }
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
                log.warn("Could not connect to ES to initialize index '{}'", indexName);
            }, "es-index-init").start();
        } catch (Exception e) {
            log.warn("ES index initialization failed: {}", e.getMessage());
        }
    }

    private void createIndex() throws Exception {
        String mappingJson = String.format("""
                {
                  "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0,
                    "analysis": {
                      "analyzer": {
                        "ik_analyzer": {
                          "type": "custom",
                          "tokenizer": "ik_max_word",
                          "filter": ["lowercase"]
                        }
                      }
                    }
                  },
                  "mappings": {
                    "properties": {
                      "content": {
                        "type": "text",
                        "analyzer": "ik_analyzer",
                        "search_analyzer": "ik_smart"
                      },
                      "content_vector": {
                        "type": "dense_vector",
                        "dims": %d,
                        "similarity": "cosine",
                        "index_options": {
                          "type": "hnsw",
                          "m": 16,
                          "ef_construction": 200
                        }
                      },
                      "section_path": {"type": "keyword"},
                      "heading_level": {"type": "integer"},
                      "parent_doc_id": {"type": "keyword"},
                      "document_type": {"type": "keyword"},
                      "evidence_level": {"type": "integer"},
                      "publication_date": {"type": "date", "format": "yyyy-MM-dd||yyyy-MM||yyyy"},
                      "source_name": {"type": "keyword"},
                      "medical_entities": {"type": "keyword"}
                    }
                  }
                }
                """, dimensions);

        esClient.indices().create(CreateIndexRequest.of(c -> c
                .index(indexName)
                .withJson(new StringReader(mappingJson))));
        log.info("ES index '{}' created with {} dimensions", indexName, dimensions);
    }
}
