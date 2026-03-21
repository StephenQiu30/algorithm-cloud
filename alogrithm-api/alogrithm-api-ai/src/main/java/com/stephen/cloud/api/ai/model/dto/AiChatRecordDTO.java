package com.stephen.cloud.api.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话记录持久化 DTO
 * <p>
 * 用于封装需要异步持久化到数据库的对话信息及 Token 用量。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 对话记录持久化对象")
public class AiChatRecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    private Long userId;

    /**
     * 会话 ID
     */
    @Schema(description = "会话 ID")
    private String sessionId;

    /**
     * 对话消息 (用户提问)
     */
    @Schema(description = "对话消息")
    private String message;

    /**
     * AI 响应内容
     */
    @Schema(description = "AI 响应内容")
    private String response;

    /**
     * 模型类型
     */
    @Schema(description = "模型类型")
    private String modelType;

    /**
     * 帖子 ID (可选，用于同步总结)
     */
    @Schema(description = "帖子 ID")
    private Long postId;

    /**
     * 总消耗 token
     */
    @Schema(description = "总消耗 token")
    private Integer totalTokens;

    /**
     * 提示消耗 token
     */
    @Schema(description = "提示消耗 token")
    private Integer promptTokens;

    /**
     * 生成消耗 token
     */
    @Schema(description = "生成消耗 token")
    private Integer completionTokens;
}
