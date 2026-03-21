package com.stephen.cloud.api.log.model.dto.email;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 邮件记录查询请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "邮件记录查询请求")
public class EmailRecordQueryRequest extends PageRequest implements Serializable {

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
     * 发送状态
     */
    @Schema(description = "发送状态")
    private String status;

    /**
     * 搜索文本
     */
    @Schema(description = "搜索文本")
    private String searchText;
}
