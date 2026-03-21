package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话响应
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 对话响应")
public class AiChatResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 回答内容
     */
    @Schema(description = "AI 回答的结果文本")
    private String content;

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
