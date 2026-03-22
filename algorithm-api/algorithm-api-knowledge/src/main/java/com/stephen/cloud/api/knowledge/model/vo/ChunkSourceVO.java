package com.stephen.cloud.api.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识切片源视图
 * <p>
 * 表示从文档库中检索出的具体文本片段及其置信度。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识切片源视图")
public class ChunkSourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 切片 ID (在向量库或关系库中的标识)
     */
    @Schema(description = "切片 ID")
    private Long chunkId;

    /**
     * 切片文本内容
     */
    @Schema(description = "切片内容")
    private String content;

    /**
     * 相似度分数 (分值越高越匹配)
     */
    @Schema(description = "相似度分数")
    private Double score;
}
