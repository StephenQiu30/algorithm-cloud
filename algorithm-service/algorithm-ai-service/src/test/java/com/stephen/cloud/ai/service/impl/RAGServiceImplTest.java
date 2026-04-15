package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.advisor.ReReadingAdvisor;
import com.stephen.cloud.ai.config.RagGenerationProperties;
import com.stephen.cloud.ai.config.RagWebSearchProperties;
import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.knowledge.retrieval.RagWebSearchFallbackDecider;
import com.stephen.cloud.ai.knowledge.retrieval.RetrievalOrchestrator;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.retrieval.model.WebSearchFallbackDecision;
import com.stephen.cloud.ai.mapper.RAGHistoryMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.api.ai.model.enums.RetrievalStrategyEnum;
import com.stephen.cloud.api.ai.model.vo.RAGStreamEventVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.CHUNK_ID;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.DOCUMENT_ID;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.DOCUMENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG 服务事件流单元测试")
class RAGServiceImplTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private RAGHistoryMapper ragHistoryMapper;

    @Mock
    private RetrievalOrchestrator retrievalOrchestrator;

    @Mock
    private ReReadingAdvisor reReadingAdvisor;

    private RAGServiceImpl ragService;

    @BeforeEach
    void setUp() throws Exception {
        ragService = new RAGServiceImpl();
        setField("chatClient", chatClient);
        setField("chatMemory", chatMemory);
        setField("ragHistoryMapper", ragHistoryMapper);
        setField("retrievalOrchestrator", retrievalOrchestrator);
        setField("ragGenerationProperties", generationProperties());
        setField("ragWebSearchProperties", webSearchProperties());
        setField("ragDocumentHelper", new RagDocumentHelper());
        setField("reReadingAdvisor", reReadingAdvisor);
        setField("ragWebSearchFallbackDecider", mock(RagWebSearchFallbackDecider.class));
        when(chatMemory.get(anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("触发联网兜底时应输出 fallback 事件并以 WEB_SEARCH_FALLBACK 落库")
    void shouldEmitFallbackEventsAndPersistWebSearchStrategy() throws Exception {
        RetrievalResult retrievalResult = new RetrievalResult();
        retrievalResult.setDocs(List.of());
        retrievalResult.setRetrievalMeta("{\"vectorHitCount\":0}");
        retrievalResult.setRetrievalStrategy(RetrievalStrategyEnum.HYBRID_RRF.getValue());
        retrievalResult.setRewriteQuery("冒泡排序复杂度");
        when(retrievalOrchestrator.retrieve(anyString(), anyLong(), any(), anyList())).thenReturn(retrievalResult);

        RagWebSearchFallbackDecider decider = getDeciderMock();
        when(decider.decide(eq(retrievalResult), eq(true))).thenReturn(new WebSearchFallbackDecision(
                true,
                WebSearchFallbackDecision.EMPTY_RECALL,
                0,
                null,
                null));

        mockStreamingAnswer("联网搜索答案");

        List<ServerSentEvent<RAGStreamEventVO>> events = ragService.askEventStream(
                        "冒泡排序的最新应用场景",
                        1L,
                        2L,
                        5,
                        "conv-1",
                        true)
                .collectList()
                .block();

        assertThat(events).isNotNull();
        assertThat(events)
                .extracting(event -> event.data() == null ? null : event.data().getType())
                .contains("fallback", "done");
        assertThat(events.stream()
                .map(ServerSentEvent::data)
                .filter(data -> data != null && "fallback".equals(data.getType()))
                .map(RAGStreamEventVO::getFallbackReason))
                .contains(WebSearchFallbackDecision.EMPTY_RECALL);

        ArgumentCaptor<RAGHistory> historyCaptor = ArgumentCaptor.forClass(RAGHistory.class);
        verify(ragHistoryMapper).insert(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRetrievalStrategy())
                .isEqualTo(RetrievalStrategyEnum.WEB_SEARCH_FALLBACK.getValue());
        assertThat(historyCaptor.getValue().getRetrievalMeta())
                .contains("\"webSearchTriggered\":true")
                .contains("\"webSearchDecisionReason\":\"EMPTY_RECALL\"");
    }

    @Test
    @DisplayName("知识库正常命中时不应输出 fallback 事件")
    void shouldNotEmitFallbackEventWhenKnowledgeContextIsEnough() throws Exception {
        Document doc = new Document("chunk-1", "冒泡排序的平均时间复杂度是 O(n^2)。",
                Map.of(CHUNK_ID, "chunk-1", DOCUMENT_ID, 11L, DOCUMENT_NAME, "冒泡排序"));
        RetrievalResult retrievalResult = new RetrievalResult();
        retrievalResult.setDocs(List.of(doc));
        retrievalResult.setRetrievalMeta("{\"vectorHitCount\":1}");
        retrievalResult.setRetrievalStrategy(RetrievalStrategyEnum.HYBRID_RRF_RERANK.getValue());
        retrievalResult.setRewriteQuery("冒泡排序复杂度");
        when(retrievalOrchestrator.retrieve(anyString(), anyLong(), any(), anyList())).thenReturn(retrievalResult);

        RagWebSearchFallbackDecider decider = getDeciderMock();
        when(decider.decide(eq(retrievalResult), eq(true))).thenReturn(new WebSearchFallbackDecision(
                false,
                WebSearchFallbackDecision.ENOUGH_CONTEXT,
                1,
                0.81D,
                0.81D));

        mockStreamingAnswer("知识库答案");

        List<ServerSentEvent<RAGStreamEventVO>> events = ragService.askEventStream(
                        "冒泡排序复杂度",
                        1L,
                        2L,
                        5,
                        "conv-2",
                        true)
                .collectList()
                .block();

        assertThat(events).isNotNull();
        assertThat(events)
                .extracting(event -> event.data() == null ? null : event.data().getType())
                .doesNotContain("fallback");

        ArgumentCaptor<RAGHistory> historyCaptor = ArgumentCaptor.forClass(RAGHistory.class);
        verify(ragHistoryMapper).insert(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRetrievalStrategy())
                .isEqualTo(RetrievalStrategyEnum.HYBRID_RRF_RERANK.getValue());
        assertThat(historyCaptor.getValue().getRetrievalMeta())
                .contains("\"webSearchTriggered\":false")
                .contains("\"webSearchDecisionReason\":\"ENOUGH_CONTEXT\"");
    }

    private RagGenerationProperties generationProperties() {
        RagGenerationProperties properties = new RagGenerationProperties();
        properties.setTemperature(0.3D);
        properties.setMaxContextLength(4000);
        return properties;
    }

    private RagWebSearchProperties webSearchProperties() {
        RagWebSearchProperties properties = new RagWebSearchProperties();
        properties.setEnabled(true);
        properties.setFallbackOnEmpty(true);
        properties.setFallbackOnLowConfidence(true);
        return properties;
    }

    private void mockStreamingAnswer(String answer) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just(answer));
    }

    private RagWebSearchFallbackDecider getDeciderMock() throws Exception {
        return (RagWebSearchFallbackDecider) getField("ragWebSearchFallbackDecider");
    }

    private void setField(String fieldName, Object value) throws Exception {
        var field = RAGServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(ragService, value);
    }

    private Object getField(String fieldName) throws Exception {
        var field = RAGServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(ragService);
    }
}
