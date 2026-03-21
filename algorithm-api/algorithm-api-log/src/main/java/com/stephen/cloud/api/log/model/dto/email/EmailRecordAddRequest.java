package com.stephen.cloud.api.log.model.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 邮件记录创建请求
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮件记录创建请求")
public class EmailRecordAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @Schema(description = "记录ID")
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
}
