package com.stephen.cloud.ai.knowledge.retrieval;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.stephen.cloud.ai.config.RagWebSearchProperties;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.retrieval.model.WebSearchFallbackDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.KEYWORD_SCORE;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.VECTOR_SCORE;
import static org.assertj.core.api.Assertions.assertThat;

class RagWebSearchFallbackDeciderTest {

    private RagWebSearchFallbackDecider decider;

    @BeforeEach
    void setUp() {
        RagWebSearchProperties properties = new RagWebSearchProperties();
        properties.setEnabled(true);
        properties.setFallbackOnEmpty(true);
        properties.setFallbackOnLowConfidence(true);
        properties.setMinKnowledgeHits(2);
        properties.setMinTopSimilarity(0.72D);
        properties.setMinAverageSimilarity(0.65D);
        decider = new RagWebSearchFallbackDecider(properties, new RagDocumentHelper());
    }

    @Test
    void shouldFallbackWhenRecallIsEmpty() {
        RetrievalResult result = new RetrievalResult();
        result.setDocs(List.of());

        WebSearchFallbackDecision decision = decider.decide(result, true);

        assertThat(decision.shouldFallback()).isTrue();
        assertThat(decision.reason()).isEqualTo(WebSearchFallbackDecision.EMPTY_RECALL);
    }

    @Test
    void shouldFallbackWhenRecallIsLowConfidence() {
        RetrievalResult result = new RetrievalResult();
        result.setDocs(List.of(vectorDoc("chunk-1", 0.61D)));

        WebSearchFallbackDecision decision = decider.decide(result, true);

        assertThat(decision.shouldFallback()).isTrue();
        assertThat(decision.reason()).isEqualTo(WebSearchFallbackDecision.LOW_CONFIDENCE_RECALL);
        assertThat(decision.topVectorSimilarity()).isEqualTo(0.61D);
    }

    @Test
    void shouldFallbackWhenOnlyKeywordSignalExistsAndHitsAreInsufficient() {
        RetrievalResult result = new RetrievalResult();
        result.setDocs(List.of(keywordDoc("chunk-1", 9.8D)));

        WebSearchFallbackDecision decision = decider.decide(result, true);

        assertThat(decision.shouldFallback()).isTrue();
        assertThat(decision.reason()).isEqualTo(WebSearchFallbackDecision.KEYWORD_ONLY_LOW_COVERAGE);
    }

    @Test
    void shouldNotFallbackWhenEnoughKnowledgeHitsExist() {
        RetrievalResult result = new RetrievalResult();
        result.setDocs(List.of(vectorDoc("chunk-1", 0.58D), vectorDoc("chunk-2", 0.57D)));

        WebSearchFallbackDecision decision = decider.decide(result, true);

        assertThat(decision.shouldFallback()).isFalse();
        assertThat(decision.reason()).isEqualTo(WebSearchFallbackDecision.ENOUGH_CONTEXT);
        assertThat(decision.knowledgeHitCount()).isEqualTo(2);
    }

    @Test
    void shouldNotFallbackWhenEnoughKeywordHitsExist() {
        RetrievalResult result = new RetrievalResult();
        result.setDocs(List.of(keywordDoc("chunk-1", 9.8D), keywordDoc("chunk-2", 8.7D)));

        WebSearchFallbackDecision decision = decider.decide(result, true);

        assertThat(decision.shouldFallback()).isFalse();
        assertThat(decision.reason()).isEqualTo(WebSearchFallbackDecision.ENOUGH_CONTEXT);
        assertThat(decision.knowledgeHitCount()).isEqualTo(2);
    }

    @Test
    void shouldRespectRequestLevelDisable() {
        RetrievalResult result = new RetrievalResult();
        result.setDocs(List.of());

        WebSearchFallbackDecision decision = decider.decide(result, false);

        assertThat(decision.shouldFallback()).isFalse();
        assertThat(decision.reason()).isEqualTo(WebSearchFallbackDecision.REQUEST_DISABLED);
    }

    @Test
    void shouldSupportDashScopeEnableSearchOption() {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(DashScopeChatOptions.builder().build());

        assertThat(beanWrapper.isWritableProperty("enableSearch")).isTrue();
    }

    private Document vectorDoc(String id, double similarity) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(VECTOR_SCORE, similarity);
        return new Document(id, "vector hit", metadata);
    }

    private Document keywordDoc(String id, double keywordScore) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(KEYWORD_SCORE, keywordScore);
        return new Document(id, "keyword hit", metadata);
    }
}
