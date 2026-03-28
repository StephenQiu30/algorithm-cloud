package com.stephen.cloud.api.ai.model.vo;

import com.stephen.cloud.api.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库视图对象
 * 用于API数据传输
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "知识库视图对象")
public class KnowledgeBaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "知识库ID")
    private Long id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "知识库描述")
    private String description;

    @Schema(description = "文档数量")
    private Integer documentCount;

    @Schema(description = "创建用户")
    private UserVO userVO;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
