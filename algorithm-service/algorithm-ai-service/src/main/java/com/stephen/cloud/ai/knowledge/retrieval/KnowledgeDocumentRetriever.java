package com.stephen.cloud.ai.knowledge.retrieval;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.context.RagSearchContext;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 知识文档检索器实现：基于 Spring AI 的 DocumentRetriever。
 * 直接调用 VectorSearchManager 执行底层检索。
 */
@Component
public class KnowledgeDocumentRetriever implements DocumentRetriever {

    public static final String KNOWLEDGE_BASE_ID_CONTEXT_KEY = "knowledgeBaseId";
    public static final String TOP_K_CONTEXT_KEY = "topK";

    @Resource
    private VectorSearchManager vectorSearchManager;

    @Resource
    private KnowledgeSearchRequestBuilder knowledgeSearchRequestBuilder;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    @Override
    public List<Document> retrieve(Query query) {
        if (query == null || StringUtils.isBlank(query.text())) {
            return Collections.emptyList();
        }

        Map<String, Object> ctx = query.context();
        Object kbObj = ctx != null ? ctx.get(KNOWLEDGE_BASE_ID_CONTEXT_KEY) : null;
        Long kbId = kbObj == null ? null : (kbObj instanceof Number n ? n.longValue() : Long.valueOf(kbObj.toString()));
        if (kbId == null || kbId <= 0) {
            return Collections.emptyList();
        }

        Integer requestTopK = null;
        if (ctx != null) {
            Object topKObj = ctx.get(TOP_K_CONTEXT_KEY);
            if (topKObj instanceof Number n) {
                requestTopK = n.intValue();
            } else if (topKObj != null) {
                try {
                    requestTopK = Integer.valueOf(topKObj.toString());
                } catch (NumberFormatException ignored) {
                    requestTopK = null;
                }
            }
        }

        SearchRequest searchRequest = knowledgeSearchRequestBuilder.build(query.text(), kbId, requestTopK);

        // 3. 决定检索模式 (kNN 或 Hybrid)
        VectorSimilarityModeEnum mode = knowledgeProperties.isHybridSearchEnabled()
                ? VectorSimilarityModeEnum.HYBRID
                : VectorSimilarityModeEnum.KNN;

        // 4. 执行检索
        List<Document> documents = vectorSearchManager.search(searchRequest, mode);

        // 5. 将结果同步到 RAG 检索上下文并返回
        if (documents != null && !documents.isEmpty()) {
            RagSearchContext.addSources(vectorSearchManager.mapToVO(documents));
        }

        return documents != null ? documents : Collections.emptyList();
    }
}
