package com.stephen.cloud.api.log.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 邮件记录VO
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "邮件记录")
public class EmailRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
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
     * 是否HTML
     */
    @Schema(description = "是否HTML")
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
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;
}
