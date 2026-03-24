package com.stephen.cloud.api.ai.model.dto.knowledgebase;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库查询请求")
public class KnowledgeBaseQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "知识库ID")
    private Long id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "搜索词")
    private String searchText;

    @Schema(description = "创建用户ID")
    private Long userId;
}
