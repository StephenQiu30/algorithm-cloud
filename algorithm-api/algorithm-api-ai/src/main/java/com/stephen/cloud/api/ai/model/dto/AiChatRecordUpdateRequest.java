package com.stephen.cloud.api.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新 AI 对话记录请求 (管理员)
 */
@Data
@Schema(description = "更新 AI 对话记录请求")
public class AiChatRecordUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

    /**
     * 对话消息
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
}
