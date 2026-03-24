package com.stephen.cloud.ai.enums;

import lombok.Getter;

@Getter
public enum RagMetricEnum {

    RAG_LATENCY_MS("rag_latency_ms"),
    RAG_RETRIEVAL_LATENCY_MS("rag_retrieval_latency_ms"),
    RAG_RETRIEVAL_HIT_COUNT("rag_retrieval_hit_count"),
    RAG_RETRIEVAL_FALLBACK_COUNT("rag_retrieval_fallback_count"),
    RAG_TOKEN_TOTAL("rag_token_total"),
    KEYWORD_EXTRACTION_FAILURE_COUNT("keyword_extraction_failure_count");

    private final String value;

    RagMetricEnum(String value) {
        this.value = value;
    }
}
