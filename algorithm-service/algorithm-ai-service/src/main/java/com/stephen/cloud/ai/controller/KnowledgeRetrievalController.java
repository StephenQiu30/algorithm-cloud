package com.stephen.cloud.ai.controller;

import com.stephen.cloud.ai.service.KnowledgeRetrievalService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识检索接口
 * <p>
 * 提供面向管理员或开发者的知识检索诊断能力。
 * 支持在指定知识库内执行语义搜索，并查看向量匹配评分及源片段。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/retrieval")
@Tag(name = "KnowledgeRetrievalController", description = "知识检索接口")
public class KnowledgeRetrievalController {

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

    /**
     * 知识库检索
     *
     * @param knowledgeRetrievalRequest 检索请求
     * @return 匹配的切片列表
     */
    @PostMapping("/search")
    @Operation(summary = "检索知识库内容", description = "获取特定查询在知识库中的相似切片和评分")
    public BaseResponse<List<ChunkSourceVO>> search(@RequestBody KnowledgeRetrievalRequest knowledgeRetrievalRequest) {
        Long loginUserId = SecurityUtils.getLoginUserId();
        List<ChunkSourceVO> result = knowledgeRetrievalService.search(knowledgeRetrievalRequest, loginUserId);
        return ResultUtils.success(result);
    }
}
