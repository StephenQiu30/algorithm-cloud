package com.stephen.cloud.ai.knowledge.rewrite;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 查询改写服务
 * <p>
 * 对用户原始问题进行语义改写、关键词提取、同义词扩展等处理，
 * 提升检索召回质量
 * </p>
 *
 * @author StephenQiu30
 */
public interface QueryRewriteService {

    default RewriteResult rewrite(String question) {
        return rewrite(question, List.of());
    }

    /**
     * 执行查询改写
     *
     * @param question 用户原始问题
     * @param history  会话历史（用于多轮对话场景）
     * @return 改写结果
     */
    RewriteResult rewrite(String question, List<Message> history);
}
