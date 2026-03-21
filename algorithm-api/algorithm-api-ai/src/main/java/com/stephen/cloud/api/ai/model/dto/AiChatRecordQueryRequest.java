package com.stephen.cloud.api.ai.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话记录查询请求
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
     * 模型类型
     */
    @Schema(description = "模型类型")
    private String modelType;

    /**
     * 搜索词
     */
    @Schema(description = "搜索词 (匹配消息内容或响应)")
    private String searchText;
}
