package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;

public interface KnowledgeIngestService {

    void ingestDocument(KnowledgeDocIngestMessage message);
}
