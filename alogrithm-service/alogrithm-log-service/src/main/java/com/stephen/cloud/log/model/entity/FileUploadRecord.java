package com.stephen.cloud.log.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文件上传记录
 * <p>
 * 记录所有文件上传情况，支持腾讯云 COS 存储类型
 * 记录文件元信息和访问地址
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "file_upload_record")
@Data
@Schema(description = "文件上传记录表")
public class FileUploadRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "记录ID")
    private Long id;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    private Long userId;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String fileName;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件后缀
     */
    @Schema(description = "文件后缀")
    private String fileSuffix;

    /**
     * 内容类型
     */
    @Schema(description = "内容类型")
    private String contentType;

    /**
     * 存储类型
     */
    @Schema(description = "存储类型")
    private String storageType;

    /**
     * 存储桶
     */
    @Schema(description = "存储桶")
    private String bucket;

    /**
     * 对象键/路径
     */
    @Schema(description = "对象键/路径")
    private String objectKey;

    /**
     * 访问URL
     */
    @Schema(description = "访问URL")
    private String url;

    /**
     * 文件MD5
     */
    @Schema(description = "文件MD5")
    private String md5;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    private String clientIp;

    /**
     * 上传状态
     */
    @Schema(description = "上传状态")
    private String status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

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
}
