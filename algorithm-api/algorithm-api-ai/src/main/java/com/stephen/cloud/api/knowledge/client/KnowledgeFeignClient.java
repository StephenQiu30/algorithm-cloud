package com.stephen.cloud.api.knowledge.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.dto.rag.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "algorithm-ai-service", path = "/api/ai/knowledge", contextId = "knowledgeFeignClient")
public interface KnowledgeFeignClient {

    @PostMapping("/add")
    BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBaseAddRequest request);

    @PostMapping("/list/page/vo")
    BaseResponse<Page<KnowledgeBaseVO>> listKnowledgeBaseVOByPage(@RequestBody KnowledgeBaseQueryRequest request);

    @GetMapping("/{kbId}/document/{docId}")
    BaseResponse<KnowledgeDocumentVO> getDocument(@PathVariable("kbId") Long kbId,
                                                  @PathVariable("docId") Long docId);

    @PostMapping("/{kbId}/chat")
    BaseResponse<RagChatResponseVO> ragChat(@PathVariable("kbId") Long kbId, @RequestBody RagChatRequest request);
}
