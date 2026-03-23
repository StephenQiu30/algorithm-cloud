package com.stephen.cloud.api.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 知识库检索响应视图
 * 
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库检索响应视图")
public class KnowledgeSearchVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "检索到的知识分片列表")
    private List<ChunkSourceVO> sources;
}

