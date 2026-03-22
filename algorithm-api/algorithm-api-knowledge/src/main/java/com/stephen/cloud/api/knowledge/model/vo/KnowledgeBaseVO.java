package com.stephen.cloud.api.knowledge.model.vo;

import com.stephen.cloud.api.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库视图
 * <p>
 * 展现给前端的知识库脱敏信息，包括用户信息。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库视图")
public class KnowledgeBaseVO implements Serializable {

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
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 知识库描述
     */
    @Schema(description = "知识库描述")
    private String description;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private UserVO userVO;
}
