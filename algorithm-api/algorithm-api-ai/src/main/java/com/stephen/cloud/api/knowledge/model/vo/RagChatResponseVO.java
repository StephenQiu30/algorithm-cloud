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
 * RAG 对话响应视图
 * <p>
 * 模型生成的回答内容，以及对应的检索源资料及其评分。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 对话响应视图")
public class RagChatResponseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * AI 回答内容
     */
    @Schema(description = "AI 回答内容")
    private String answer;

    /**
     * 参考资料源 (文档切片)
     */
    @Schema(description = "参考资料源")
    private List<ChunkSourceVO> sources;
}
