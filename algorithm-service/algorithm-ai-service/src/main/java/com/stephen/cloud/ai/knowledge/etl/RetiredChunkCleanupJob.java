package com.stephen.cloud.ai.knowledge.etl;

import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Soft-deleted inventory is the durable retry queue for retired index cleanup. */
@Slf4j
@Component
public class RetiredChunkCleanupJob {
    @Resource private DocumentChunkMapper mapper;
    @Resource private VectorStore vectorStore;
    @Resource private KeywordIndexPublisher keywordIndexPublisher;
    @Scheduled(fixedDelay = 600000)
    public void retryCleanup() {
        var retired = mapper.selectRetiredForCleanup();
        if (retired == null || retired.isEmpty()) return;
        try {
            var vectors = retired.stream().map(DocumentChunk::getVectorId).filter(id -> id != null && !id.isBlank()).distinct().toList();
            if (!vectors.isEmpty()) vectorStore.delete(vectors);
            keywordIndexPublisher.delete(retired);
            mapper.deleteRetired(retired.stream().map(DocumentChunk::getId).toList());
        } catch (Exception e) {log.warn("旧索引清理失败，保留持久化重试记录", e);}
    }
}
