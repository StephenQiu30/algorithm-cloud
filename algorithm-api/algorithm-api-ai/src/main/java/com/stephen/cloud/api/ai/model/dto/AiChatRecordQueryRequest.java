package com.stephen.cloud.api.ai.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话记录查询请求
 * <p>
 * 支持根据 ID、会话 ID、用户 ID、模型类型以及关键字对对话内容和响应进行全面搜索。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI 对话记录查询请求")
public class AiChatRecordQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
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
     * 模型类型 (如 dashscope, ollama 等)
     */
    @Schema(description = "模型类型")
    private String modelType;

    /**
     * 搜索关键词 (模糊查询 message 或 response)
     */
    @Schema(description = "搜索关键词")
    private String searchText;
}
