package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文档表
 *
 * @author StephenQiu30
 * @TableName document
 */
@TableName(value = "document")
@Data
@Schema(description = "文档表")
public class Document implements Serializable {

    /**
     * 文档ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "文档ID")
    private Long id;

    /**
     * 知识库ID
     */
    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    /**
     * 文档名称
     */
    @Schema(description = "文档名称")
    private String name;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 文件扩展名
     */
    @Schema(description = "文件扩展名")
    private String fileExtension;

    /**
     * 处理状态：PENDING/PROCESSING/COMPLETED/FAILED/TIMEOUT
     */
    @Schema(description = "处理状态：PENDING/PROCESSING/COMPLETED/FAILED/TIMEOUT")
    private String status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 分片数量
     */
    @Schema(description = "分片数量")
    private Integer chunkCount;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "业务标签")
    private String bizTag;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "扩展元数据")
    private String extraMeta;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    private Long userId;

    /**
     * 上传时间
     */
    @Schema(description = "上传时间")
    private Date uploadTime;

    /**
     * 开始处理时间
     */
    @Schema(description = "开始处理时间")
    private Date processStartTime;

    /**
     * 处理完成时间
     */
    @Schema(description = "处理完成时间")
    private Date processEndTime;

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

    /**
     * 是否删除
     */
    @TableLogic
    @Schema(description = "是否删除")
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
