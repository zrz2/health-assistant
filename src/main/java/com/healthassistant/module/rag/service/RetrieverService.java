package com.healthassistant.module.rag.service;

import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RetrieverService {

    private static final Logger log = LoggerFactory.getLogger(RetrieverService.class);

    private final QueryRewriter queryRewriter;
    private final VectorSearchService vectorSearchService;
    private final KeywordSearchService keywordSearchService;
    private final HybridSearchService hybridSearchService;
    private final ReRankerService reRankerService;

    public RetrieverService(QueryRewriter queryRewriter,
                            VectorSearchService vectorSearchService,
                            KeywordSearchService keywordSearchService,
                            HybridSearchService hybridSearchService,
                            ReRankerService reRankerService) {
        this.queryRewriter = queryRewriter;
        this.vectorSearchService = vectorSearchService;
        this.keywordSearchService = keywordSearchService;
        this.hybridSearchService = hybridSearchService;
        this.reRankerService = reRankerService;
    }

    /**
     * Full retrieval pipeline:
     * Query Rewrite -> Vector + Keyword Search -> RRF Fusion -> Re-rank -> Top-K
     */
    public RetrieveResult retrieve(String query, String history, int topK) {
        // 1. Query rewriting
        QueryRewriter.RewriteResult rewrite = queryRewriter.rewrite(query, history);
        log.info("Query rewritten: {} -> {} search queries", query, rewrite.searchQueries().size());

        // 2. Parallel search (vector + keyword)
        List<RetrievedDocument> vectorDocs = vectorSearchService.search(rewrite.searchQueries(), 10);
        List<RetrievedDocument> keywordDocs = keywordSearchService.search(rewrite.rewrittenQuery(), 10);
        log.info("Retrieved: {} vector + {} keyword = {} total",
                vectorDocs.size(), keywordDocs.size(), vectorDocs.size() + keywordDocs.size());

        // 3. RRF hybrid fusion
        List<RetrievedDocument> fused = hybridSearchService.merge(vectorDocs, keywordDocs, 15);
        log.info("After RRF fusion: {} documents", fused.size());

        // 4. Re-rank (get top-K)
        List<RetrievedDocument> finalDocs = fused;
        if (fused.size() > topK) {
            finalDocs = reRankerService.rerank(query, fused, topK);
            log.info("After re-rank: {} documents", finalDocs.size());
        }

        return new RetrieveResult(rewrite, finalDocs);
    }

    public record RetrieveResult(QueryRewriter.RewriteResult rewriteResult,
                                  List<RetrievedDocument> documents) {}
}
