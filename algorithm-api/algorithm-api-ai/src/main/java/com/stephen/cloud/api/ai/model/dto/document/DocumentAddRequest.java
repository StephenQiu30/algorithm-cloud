package com.stephen.cloud.api.ai.model.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 上传文档请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "上传文档请求")
public class DocumentAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;
}
