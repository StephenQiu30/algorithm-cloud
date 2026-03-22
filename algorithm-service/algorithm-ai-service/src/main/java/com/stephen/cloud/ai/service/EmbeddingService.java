package com.stephen.cloud.ai.service;

import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

public interface EmbeddingService {

    EmbeddingResponse embedForResponse(List<String> texts);
}
