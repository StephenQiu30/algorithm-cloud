package com.stephen.cloud.api.ai.model.vo;

import com.stephen.cloud.api.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文档视图对象
 * 用于API数据传输
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "文档视图对象")
public class DocumentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long id;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件扩展名")
    private String fileExtension;

    @Schema(description = "处理状态")
    private String status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "分片数量")
    private Integer chunkCount;

    @Schema(description = "上传用户")
    private UserVO userVO;

    @Schema(description = "上传时间")
    private Date uploadTime;

    @Schema(description = "处理完成时间")
    private Date processEndTime;
}
