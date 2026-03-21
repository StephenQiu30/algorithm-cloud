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
 * 邮件记录实体
 * <p>
 * 记录所有邮件发送情况，包括发送成功和失败
 * 支持重试机制和幂等性保证
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "email_record")
@Data
@Schema(description = "邮件记录表")
public class EmailRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮件记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "邮件记录ID")
    private Long id;

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private String msgId;

    /**
     * 业务幂等ID
     */
    @Schema(description = "业务幂等ID")
    private String bizId;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 收件人邮箱
     */
    @Schema(description = "收件人邮箱")
    private String toEmail;

    /**
     * 邮件主题
     */
    @Schema(description = "邮件主题")
    private String subject;

    /**
     * 邮件内容
     */
    @Schema(description = "邮件内容")
    private String content;

    /**
     * 是否HTML格式（0-否，1-是）
     */
    @Schema(description = "是否HTML格式（0-否，1-是）")
    private Integer isHtml;

    /**
     * 发送状态
     */
    @Schema(description = "发送状态")
    private String status;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 发送渠道
     */
    @Schema(description = "发送渠道")
    private String provider;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    private Date sendTime;

    /**
     * 下次重试时间
     */
    @Schema(description = "下次重试时间")
    private Date nextRetryTime;

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
