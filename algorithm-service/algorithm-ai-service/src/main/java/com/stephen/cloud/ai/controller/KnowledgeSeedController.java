package com.stephen.cloud.ai.controller;

import com.stephen.cloud.ai.service.KnowledgeSeedService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库种子接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/knowledge/seed")
@Tag(name = "KnowledgeSeedController", description = "知识库种子初始化接口")
public class KnowledgeSeedController {

    @Resource
    private KnowledgeSeedService knowledgeSeedService;

    /**
     * 初始化排序算法教学知识库
     *
     * @return 知识库 ID
     */
    @PostMapping("/sorting")
    @Operation(summary = "初始化排序算法教学知识库", description = "自动填充高质量的排序算法文档")
    public BaseResponse<Long> seedSortingAlgorithms() {
        Long userId = SecurityUtils.getLoginUserId();
        Long kbId = knowledgeSeedService.seedSortingAlgorithms(userId);
        return ResultUtils.success(kbId);
    }
}
