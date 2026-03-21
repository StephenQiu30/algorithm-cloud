package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 对话记录
 *
 * @author StephenQiu30
 * @TableName ai_chat_record
 */
@TableName(value = "ai_chat_record")
@Data
public class AiChatRecord implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 会话 id
     */
    private String sessionId;

    /**
     * 对话消息
     */
    private String message;

    /**
     * AI 响应内容
     */
    private String response;

    /**
     * 模型类型
     */
    private String modelType;

    /**
     * 总消耗 token
     */
    private Integer totalTokens;

    /**
     * 提示消耗 token
     */
    private Integer promptTokens;

    /**
     * 生成消耗 token
     */
    private Integer completionTokens;

    /**
     * 帖子 id
     */
    private Long postId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
