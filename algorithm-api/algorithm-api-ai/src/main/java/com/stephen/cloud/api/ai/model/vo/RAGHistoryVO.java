package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "RAG历史视图对象")
public class RAGHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "历史ID")
    private Long id;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "答案")
    private String answer;

    @Schema(description = "来源")
    private List<SourceVO> sources;

    @Schema(description = "响应时间(毫秒)")
    private Long responseTime;

    @Schema(description = "创建时间")
    private Date createTime;
}
