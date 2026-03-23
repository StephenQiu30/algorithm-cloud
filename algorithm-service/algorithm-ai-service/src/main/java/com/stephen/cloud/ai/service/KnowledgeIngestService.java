package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;

/**
 * 知识文档解析入库服务接口。
 *
 * @author StephenQiu30
 */
public interface KnowledgeIngestService {

    /**
     * 执行文档入库流水线：下载、解析、切分、存储与向量化。
     *
     * @param message 入库消息对象
     */
    void ingestDocument(KnowledgeDocIngestMessage message);

    /**
     * 按文档 ID 重新投递入库任务（重试）。
     *
     * @param documentId 文档 ID
     */
    void retryIngest(Long documentId);

    /**
     * 从向量库中下线指定文档的所有向量。
     *
     * @param documentId 文档 ID
     */
    void deleteVectors(Long documentId);
}
