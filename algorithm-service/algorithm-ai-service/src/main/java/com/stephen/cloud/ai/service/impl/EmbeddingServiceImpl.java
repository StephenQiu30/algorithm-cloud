package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.service.EmbeddingService;
import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Resource
    private EmbeddingModel embeddingModel;

    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        return embeddingModel.embedForResponse(texts);
    }
}
