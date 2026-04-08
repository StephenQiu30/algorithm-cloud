package com.stephen.cloud.ai.mq.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档处理消息
 * <p>
 * 用于 MQ 异步触发文档 ETL 流程的消息体
 * </p>
 *
 * @author StephenQiu30
 */
@Data
public class DocumentProcessMessage implements Serializable {

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
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件扩展名
     */
    private String fileExtension;
}
