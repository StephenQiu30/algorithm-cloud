package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 对话记录 VO
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "AI 对话记录视图对象")
public class AiChatRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 用户 id
     */
    @Schema(description = "用户 id")
    private Long userId;

    /**
     * 会话 id
     */
    @Schema(description = "会话 id")
    private String sessionId;

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

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;
}
