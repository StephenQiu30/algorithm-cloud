package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * RAG 流式事件
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "RAG流式事件")
public class RAGStreamEventVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "事件类型，如 start/retrieval/fallback/generation/answer/error/done")
    private String type;

    @Schema(description = "当前阶段")
    private String phase;

    @Schema(description = "提示消息")
    private String message;

    @Schema(description = "流式增量内容")
    private String content;

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "知识库或最终使用的检索策略")
    private String retrievalStrategy;

    @Schema(description = "是否触发了联网搜索兜底")
    private Boolean webSearchTriggered;

    @Schema(description = "联网搜索兜底判定原因")
    private String fallbackReason;

    @Schema(description = "知识库命中数量")
    private Integer knowledgeHitCount;

    @Schema(description = "最高向量相似度")
    private Double topVectorSimilarity;

    @Schema(description = "平均向量相似度")
    private Double averageVectorSimilarity;

    @Schema(description = "是否成功完成")
    private Boolean success;

    @Schema(description = "响应耗时(毫秒)")
    private Long responseTime;

    @Schema(description = "引用来源，仅在结束事件中返回")
    private List<SourceVO> sources;
}
