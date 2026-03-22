package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;

/**
 * 知识文档入库服务（解析、清洗、分片、向量化、写库）。
 * <p>
 * 产出与 RAG 溯源对应关系：{@code document_chunk} 存明文切片；
 * ES 中每条向量文档的 metadata 携带 chunkId / knowledgeBaseId / documentId，与 {@link RagService} 返回的 sources 一致。
 * </p>
 *
 * @author StephenQiu30
 */
public interface KnowledgeIngestService {

    /**
     * 根据 MQ 消息解析文档并写入 MySQL 分片表、ES 向量索引及向量元数据表。
     *
     * @param message 包含文档 ID、存储路径等；ES 不参与 Spring 本地事务，失败时在实现类中更新文档解析状态
     */
    void ingestDocument(KnowledgeDocIngestMessage message);

    /**
     * 按文档 ID 清理 ES 向量、{@code document_chunk} 与 {@code embedding_vector} 中关联数据（重解析或删除文档时调用）
     *
     * @param documentId 知识库文档主键
     */
    void removeChunksAndVectorsForDocument(long documentId);
}
