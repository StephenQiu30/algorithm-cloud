package com.stephen.cloud.ai.mq.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DocumentProcessMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long documentId;

    private Long knowledgeBaseId;

    private String filePath;

    private String fileExtension;
}
