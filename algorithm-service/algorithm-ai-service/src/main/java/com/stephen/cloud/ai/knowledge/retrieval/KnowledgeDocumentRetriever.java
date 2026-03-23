package com.stephen.cloud.ai.knowledge.retrieval;

import com.stephen.cloud.ai.knowledge.context.RagSearchContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeDocumentRetriever implements DocumentRetriever {

    public static final String KNOWLEDGE_BASE_ID_CONTEXT_KEY = "knowledgeBaseId";
    public static final String TOP_K_CONTEXT_KEY = "topK";
 
    private final VectorSearchManager vectorSearchManager;
 
    public KnowledgeDocumentRetriever(VectorSearchManager vectorSearchManager) {
        this.vectorSearchManager = vectorSearchManager;
    }
 
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
                requestTopK = Integer.valueOf(topKObj.toString());
            }
        }
 
        // 使用管理器执行检索
        List<Document> documents = vectorSearchManager.searchDocuments(
                kbId, query.text(), requestTopK, Integer.MAX_VALUE);
 
        // 将结果同步到 RAG 捕获上下文并返回
        RagSearchContext.addSources(vectorSearchManager.mapToVO(documents));
        return documents;
    }
}

