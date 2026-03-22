package com.stephen.cloud.api.ai.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "algorithm-ai-service", path = "/api/ai", contextId = "aiFeignClient")
public interface AiFeignClient {


    /**
     * 分页获取我的会话记录
     *
     * @param queryRequest 查询请求
     * @return 记录分页
     */
    @PostMapping("/record/my/list/page/vo")
    BaseResponse<Page<AiChatRecordVO>> listMyAiChatRecordVOByPage(@RequestBody AiChatRecordQueryRequest queryRequest);

    /**
     * 删除对话记录
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/record/delete")
    BaseResponse<Boolean> deleteAiChatRecord(@RequestBody DeleteRequest deleteRequest);
}
