package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务
 *
 * @author StephenQiu30
 */
public interface AiChatService {

    /**
     * AI 对话 (标准)
     * <p>
     * 1. 校验请求参数
     * 2. 根据模型类型获取对应的 langchain4j 客户端
     * 3. 生成回答
     * 4. 记录对话历史（同步）
     *
     * @param aiChatRequest 对话请求
     * @param request       请求
     * @return 对话响应
     */
    AiChatResponse chat(AiChatRequest aiChatRequest, HttpServletRequest request);

    /**
     * AI 对话 (流式)
     * <p>
     * 1. 校验请求参数
     * 2. 获取支持流式的模型客户端
     * 3. 通过 SSE 回调发送 token
     * 4. 完成后异步记录历史
     *
     * @param aiChatRequest 对话请求
     * @param emitter       SSE 发射器
     * @param request       请求
     */
    void streamChat(AiChatRequest aiChatRequest, SseEmitter emitter, HttpServletRequest request);

}
