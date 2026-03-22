package com.stephen.cloud.api.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话请求
 * <p>
 * 封装单次 AI 对谈所需的核心参数，包括会话上下文 ID 和模型定位。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 对话请求")
public class AiChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息内容 (用户的提问)
     */
    @Schema(description = "问题内容", example = "你好，请自我介绍一下")
    private String message;

    /**
     * 模型类型 (默认为 dashscope)
     */
    @Builder.Default
    @Schema(description = "模型类型 (dashscope: 通义千问)", example = "dashscope")
    private String modelType = "dashscope";

    /**
     * 会话 id (用于维持多连贯对话状态)
     */
    @Schema(description = "会话 id")
    private String sessionId;

    /**
     * 帖子 ID (可选，在特定业务场景如图表分析中使用)
     */
    @Schema(description = "帖子 ID (用于异步同步总结)")
    private Long postId;

    /**
     * 系统提示词 (用于定义本场对话中 AI 的人格与限定规则)
     */
    @Schema(description = "系统提示词 (用于定义 AI 角色)", example = "你是一个专业的前端开发专家")
    private String systemMessage;

}
