package com.stephen.cloud.api.knowledge.model.dto.knowledgedocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档解析消息 (RabbitMQ)
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocIngestMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 知识库 ID
     */
    private Long knowledgeBaseId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 存储路径 (COS URL)
     */
    private String storagePath;
}

