package com.healthassistant.module.rag.service;

import com.healthassistant.common.constant.Constants;
import com.healthassistant.module.rag.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final RetrieverService retrieverService;
    private final ContextBuilder contextBuilder;

    @Value("${app.rag.enabled:false}")
    private boolean ragEnabled;

    @Value("${app.rag.top-k:5}")
    private int topK;

    public RagService(RetrieverService retrieverService, ContextBuilder contextBuilder) {
        this.retrieverService = retrieverService;
        this.contextBuilder = contextBuilder;
    }

    /**
     * Generate a RAG-augmented prompt. Returns null if RAG is disabled or no documents found.
     */
    public RagResult augmentPrompt(String question, String history) {
        if (!ragEnabled) {
            return null;
        }

        try {
            // Full retrieval pipeline
            RetrieverService.RetrieveResult result = retrieverService.retrieve(question, history, topK);

            if (result.documents().isEmpty()) {
                log.info("No relevant documents found for: {}", question);
                return null;
            }

            // Build context from retrieved documents with parent expansion
            String context = contextBuilder.buildWithParentContext(result.documents());
            List<String> sources = contextBuilder.extractSources(result.documents());

            // Build RAG-augmented prompt
            String prompt = buildRagPrompt(question, history, context);

            log.info("RAG augment: {} documents, {} sources", result.documents().size(), sources.size());
            return new RagResult(prompt, result, sources);
        } catch (Exception e) {
            log.error("RAG pipeline failed, falling back to base prompt", e);
            return null;
        }
    }

    private String buildRagPrompt(String question, String history, String context) {
        return String.format("""
                你是一个专业的医疗健康助手。请基于以下医学知识参考回答用户的问题。

                ## 医学知识参考
                %s

                ## 对话历史
                %s

                ## 当前问题
                %s

                ## 回答规则
                1. 优先基于上述医学知识参考回答问题
                2. 如果知识与问题相关，引用文献编号（如【文献1】）
                3. 如果提供了用户健康档案，必须结合档案信息给出个性化建议，档案信息优先于通用知识
                4. 如果没有足够信息，诚实说明并建议咨询医生
                5. 你不是医生，不能进行诊断，回答仅供参考
                6. 保持专业、客观、严谨

                ## 免责声明
                %s
                """, context, history, question, Constants.MEDICAL_DISCLAIMER);
    }

    public record RagResult(String prompt, RetrieverService.RetrieveResult retrieveResult,
                             List<String> sources) {}
}
