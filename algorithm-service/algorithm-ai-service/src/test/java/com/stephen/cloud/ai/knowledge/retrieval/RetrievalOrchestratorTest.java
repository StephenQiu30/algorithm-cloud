package com.stephen.cloud.ai.knowledge.retrieval;

import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.knowledge.rerank.RerankService;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.rewrite.QueryRewriteService;
import com.stephen.cloud.ai.knowledge.rewrite.RewriteResult;
import com.stephen.cloud.ai.service.KeywordSearchService;
import com.stephen.cloud.ai.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG 检索编排器集成测试")
class RetrievalOrchestratorTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private KeywordSearchService keywordSearchService;

    @Mock
    private RRFFusionService rrfFusionService;

    @Mock
    private QueryRewriteService queryRewriteService;

    @Mock
    private RerankService rerankService;

    @Mock
    private RagRetrievalProperties ragRetrievalProperties;

    @Mock
    private RagDocumentHelper ragDocumentHelper;

    @Mock
    private Executor aiAsyncExecutor;

    @InjectMocks
    private RetrievalOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // 模拟默认配置
        lenient().when(ragRetrievalProperties.getTopK()).thenReturn(5);
        lenient().when(ragRetrievalProperties.getVectorWeight()).thenReturn(0.7);
        lenient().when(ragRetrievalProperties.getKeywordWeight()).thenReturn(0.3);
        lenient().when(ragRetrievalProperties.getRetrievalTimeoutSeconds()).thenReturn(3);
        lenient().when(ragRetrievalProperties.isRewriteEnabled()).thenReturn(true);
        lenient().when(ragRetrievalProperties.isRerankEnabled()).thenReturn(true);
        lenient().when(ragRetrievalProperties.getRerankTopN()).thenReturn(5);

        // 模拟异步执行器（直接运行，简化测试）
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(aiAsyncExecutor).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("测试完整的 6 阶段 RAG 检索链路")
    void testCompleteRetrievalPipeline() {
        String question = "冒泡排序的时间复杂度?";
        
        // 1. 模拟 Query 改写
        RewriteResult rewriteResult = new RewriteResult();
        rewriteResult.setSemanticQuery("冒泡排序 时间复杂度");
        rewriteResult.setKeywordQuery("冒泡排序 时间复杂度");
        when(queryRewriteService.rewrite(anyString(), anyList())).thenReturn(rewriteResult);

        // 2. 模拟并行检索结果
        Document doc1 = new Document("向量路召回文本", Map.of("id", "v1"));
        Document doc2 = new Document("关键词召回文本", Map.of("id", "k1"));
        when(vectorStoreService.similaritySearch(anyString(), any(), anyInt(), any())).thenReturn(List.of(doc1));
        when(keywordSearchService.bm25Search(anyString(), anyInt(), any())).thenReturn(List.of(doc2));

        // 3. 模拟 RRF 融合
        when(rrfFusionService.fuse(anyList(), anyList(), anyInt(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of(doc1, doc2));

        // 4. 模拟 Rerank 重排
        when(rerankService.rerank(anyList(), anyString(), nullable(List.class), nullable(Map.class), anyInt()))
                .thenReturn(List.of(doc1));

        // 执行核心逻辑
        RetrievalResult result = orchestrator.retrieve(question, 1L, 5);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getDocs().size());
        assertEquals(doc1, result.getDocs().get(0));
        
        // 验证各阶段是否被调用
        verify(queryRewriteService).rewrite(eq(question), anyList());
        verify(vectorStoreService).similaritySearch(eq("冒泡排序 时间复杂度"), any(), anyInt(), any());
        verify(keywordSearchService).bm25Search(eq("冒泡排序 时间复杂度"), anyInt(), any());
        verify(rrfFusionService).fuse(anyList(), anyList(), anyInt(), anyInt(), anyDouble(), anyDouble());
        verify(rerankService).rerank(anyList(), eq(question), nullable(List.class), nullable(Map.class), anyInt());
    }
}
