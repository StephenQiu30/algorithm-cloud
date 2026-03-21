package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 模型 VO
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 模型信息")
public class AiModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型名称 (如 dashscope)
     */
    @Schema(description = "模型名称", example = "dashscope")
    private String name;

    /**
     * 模型描述
     */
    @Schema(description = "模型描述", example = "通义千问")
    private String description;
}
