package com.stephen.cloud.ai.service;

/**
 * 知识库种子服务
 * <p>
 * 用于快速初始化或更新特定领域的知识库内容（如排序算法教学）。
 * </p>
 *
 * @author StephenQiu30
 */
public interface KnowledgeSeedService {

    /**
     * 播种排序算法教学知识库
     * <p>
     * 如果不存在则创建“排序算法教学”知识库，并填充核心算法文档。
     * </p>
     *
     * @param userId 操作用户 ID（通常为管理员）
     * @return 知识库 ID
     */
    Long seedSortingAlgorithms(Long userId);
}
